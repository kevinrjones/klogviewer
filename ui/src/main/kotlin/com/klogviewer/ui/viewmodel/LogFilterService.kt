package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object LogFilterService {
    private const val MISSING_BUCKET_VALUE = "(missing)"
    private const val TIME_FILTER_TOLERANCE_SECONDS = 1L
    private val NUMERIC_LITERAL_PATTERN = Regex("^-?\\d+(\\.\\d+)?$")
    private val queryPathResolver: QueryPathResolver = CanonicalQueryPathResolver()
    private val fieldPredicateEvaluator = FieldPredicateEvaluator()

    suspend fun filter(window: LogWindow): List<LogEntry> = withContext(Dispatchers.Default) {
        val timeRange = TimeRangeFilterSupport.resolveRange(window)
        val filtered = window.logs.filter { entry ->
            val isHiddenSource = entry.sourceId != null && window.hiddenSourceIds.contains(entry.sourceId)
            val matchesLevel = LevelFilterPolicy.matches(entry, window.levelFilters)
            val matchesFilter = if (window.filterQueries.isEmpty()) {
                true
            } else {
                window.filterQueries.all { query ->
                    matchesQuery(entry, query)
                }
            }
            val matchesTimeRange = timeRange?.let { (from, to) ->
                val entryInstant = TimeRangeFilterSupport.entryInstant(entry) ?: return@let false
                val toleratedFrom = lowerBoundWithTolerance(from)
                val toleratedTo = upperBoundWithTolerance(to)
                !entryInstant.isBefore(toleratedFrom) && !entryInstant.isAfter(toleratedTo)
            } ?: true
            !isHiddenSource && matchesLevel && matchesFilter && matchesTimeRange
        }

        if (window.isReversed) filtered.reversed() else filtered
    }

    private fun matchesQuery(entry: LogEntry, query: String): Boolean {
        return matchesExpression(
            entry = entry,
            expression = LogQueryParser.parse(query)
        )
    }

    private fun matchesExpression(
        entry: LogEntry,
        expression: LogQueryExpression
    ): Boolean {
        return when (expression) {
            is LogQueryExpression.TextQuery -> {
                matchesLegacyTextQuery(entry = entry, query = expression.text)
            }

            is LogQueryExpression.LegacyDashboardFieldQuery -> {
                matchesLegacyDashboardFieldQuery(entry = entry, query = expression)
            }

            is LogQueryExpression.FieldPredicate -> {
                matchesFieldPredicate(entry = entry, predicate = expression)
            }

            is LogQueryExpression.BooleanExpression -> {
                val leftMatches = matchesExpression(entry = entry, expression = expression.left)
                val rightMatches = matchesExpression(entry = entry, expression = expression.right)
                when (expression.operator) {
                    BooleanOperator.AND -> leftMatches && rightMatches
                    BooleanOperator.OR -> leftMatches || rightMatches
                }
            }
        }
    }

    private fun matchesLegacyTextQuery(entry: LogEntry, query: String): Boolean {
        return entry.content.value.contains(query, ignoreCase = true) ||
            entry.timestamp.value.contains(query, ignoreCase = true)
    }

    private fun matchesLegacyDashboardFieldQuery(
        entry: LogEntry,
        query: LogQueryExpression.LegacyDashboardFieldQuery
    ): Boolean {
        val fieldValue = entry.fields[query.field] ?: MISSING_BUCKET_VALUE
        return fieldValue.contains(query.value, ignoreCase = true)
    }

    private fun matchesFieldPredicate(
        entry: LogEntry,
        predicate: LogQueryExpression.FieldPredicate
    ): Boolean {
        val fieldValues = resolveFieldValues(
            entry = entry,
            path = predicate.path,
            isExplicitFieldPath = predicate.explicitFieldPrefix
        )
        val fieldExists = pathExists(
            entry = entry,
            path = predicate.path,
            isExplicitFieldPath = predicate.explicitFieldPrefix
        )
        return fieldPredicateEvaluator.matches(
            predicate = predicate,
            fieldValues = fieldValues,
            fieldExists = fieldExists
        )
    }

    private fun resolveFieldValues(
        entry: LogEntry,
        path: String,
        isExplicitFieldPath: Boolean
    ): List<ResolvedFieldValue> {
        val resolvedValues = mutableListOf<ResolvedFieldValue>()
        val candidatePaths = candidatePaths(
            path = path,
            isExplicitFieldPath = isExplicitFieldPath
        )
        val structuredPathIndex = entry.structuredData?.flatPathIndex.orEmpty()
        val compatibilityFields = entry.compatibilityFields()

        candidatePaths.forEach { candidatePath ->
            resolveStructuredValuesForCandidate(
                structuredPathIndex = structuredPathIndex,
                candidatePath = candidatePath
            )
                .mapTo(resolvedValues) { structuredValue ->
                    structuredValue.toResolvedFieldValue(numericLiteralPattern = NUMERIC_LITERAL_PATTERN)
                }

            resolveCompatibilityFieldValue(
                compatibilityFields = compatibilityFields,
                candidatePath = candidatePath
            )
                ?.let { value -> parseRawFieldValue(value, numericLiteralPattern = NUMERIC_LITERAL_PATTERN) }
                ?.let { resolvedValues += it }
        }

        if (!isExplicitFieldPath && path.equals("message", ignoreCase = true)) {
            resolvedValues += ResolvedFieldValue.StringValue(entry.content.value)
        }
        if (!isExplicitFieldPath && path.equals("level", ignoreCase = true)) {
            resolvedValues += ResolvedFieldValue.StringValue(entry.resolvedLevelKey())
        }

        return resolvedValues.distinct()
    }

    private fun pathExists(
        entry: LogEntry,
        path: String,
        isExplicitFieldPath: Boolean
    ): Boolean {
        val loweredPath = path.lowercase()
        if (!isExplicitFieldPath && (loweredPath == "message" || loweredPath == "level")) {
            return true
        }

        val candidatePaths = candidatePaths(
            path = path,
            isExplicitFieldPath = isExplicitFieldPath
        )
        val structuredPathIndex = entry.structuredData?.flatPathIndex.orEmpty()
        val compatibilityFields = entry.compatibilityFields()

        return candidatePaths.any { candidatePath ->
            containsResolvedPath(
                structuredPathIndex = structuredPathIndex,
                compatibilityFields = compatibilityFields,
                candidatePath = candidatePath
            )
        }
    }

    private fun candidatePaths(path: String, isExplicitFieldPath: Boolean): List<String> {
        return queryPathResolver.candidatePaths(
            path = path,
            isExplicitFieldPath = isExplicitFieldPath
        )
    }

    private fun lowerBoundWithTolerance(bound: Instant): Instant {
        return runCatching { bound.minusSeconds(TIME_FILTER_TOLERANCE_SECONDS) }
            .getOrElse { bound }
    }

    private fun upperBoundWithTolerance(bound: Instant): Instant {
        return runCatching { bound.plusSeconds(TIME_FILTER_TOLERANCE_SECONDS) }
            .getOrElse { bound }
    }

}
