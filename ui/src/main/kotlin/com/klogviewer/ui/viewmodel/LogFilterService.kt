package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.domain.model.asDisplayString
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant

object LogFilterService {
    private const val MISSING_BUCKET_VALUE = "(missing)"
    private const val TIME_FILTER_TOLERANCE_SECONDS = 1L
    private val NUMERIC_LITERAL_PATTERN = Regex("^-?\\d+(\\.\\d+)?$")
    private val CANONICAL_ALIAS_PATHS = mapOf(
        "trace.id" to listOf("trace.id", "traceId", "TraceId", "@tr"),
        "level" to listOf("level", "Level", "@l"),
        "message" to listOf("message", "Message", "msg", "@m")
    )

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
        return when (predicate.operator) {
            FieldOperator.EXISTS -> fieldExists
            FieldOperator.MISSING -> !fieldExists
            FieldOperator.IS_NULL -> {
                // NOTE: null-vs-missing is precise for structured values; compatibility string projections may
                // collapse unknowns to text and are treated as best-effort `"null"` matching.
                fieldValues.any { value -> value is ResolvedFieldValue.NullValue }
            }

            FieldOperator.EQUALS -> {
                val literal = predicate.value ?: return false
                matchesEquals(
                    values = fieldValues,
                    literal = literal,
                    path = predicate.path,
                    isExplicitFieldPath = predicate.explicitFieldPrefix
                )
            }

            FieldOperator.CONTAINS -> {
                val literal = predicate.value?.toSearchToken() ?: return false
                fieldValues.any { value ->
                    value.toSearchToken().contains(literal, ignoreCase = true)
                }
            }

            FieldOperator.REGEX -> {
                val pattern = predicate.value?.toSearchToken() ?: return false
                val regex = runCatching {
                    Regex(pattern, setOf(RegexOption.IGNORE_CASE))
                }.getOrNull() ?: return false

                fieldValues.any { value -> regex.containsMatchIn(value.toSearchToken()) }
            }

            FieldOperator.GREATER_THAN,
            FieldOperator.GREATER_THAN_OR_EQUAL,
            FieldOperator.LESS_THAN,
            FieldOperator.LESS_THAN_OR_EQUAL -> {
                val literalNumber = predicate.value.toComparableNumber() ?: return false
                fieldValues.any { value ->
                    val fieldNumber = value.toComparableNumber() ?: return@any false
                    when (predicate.operator) {
                        FieldOperator.GREATER_THAN -> fieldNumber > literalNumber
                        FieldOperator.GREATER_THAN_OR_EQUAL -> fieldNumber >= literalNumber
                        FieldOperator.LESS_THAN -> fieldNumber < literalNumber
                        FieldOperator.LESS_THAN_OR_EQUAL -> fieldNumber <= literalNumber
                        else -> false
                    }
                }
            }
        }
    }

    private fun matchesEquals(
        values: List<ResolvedFieldValue>,
        literal: QueryLiteral,
        path: String,
        isExplicitFieldPath: Boolean
    ): Boolean {
        return when (literal) {
            is QueryLiteral.StringValue -> {
                if (!isExplicitFieldPath && path.equals("level", ignoreCase = true)) {
                    values.any { value -> value.toSearchToken().equals(literal.value, ignoreCase = true) }
                } else {
                    values.any { value -> value.toSearchToken() == literal.value }
                }
            }

            is QueryLiteral.NumberValue -> {
                values.any { value -> value.toComparableNumber() == literal.value }
            }

            is QueryLiteral.BooleanValue -> {
                values.any { value -> value.toComparableBoolean() == literal.value }
            }

            QueryLiteral.NullValue -> {
                values.any { value -> value is ResolvedFieldValue.NullValue }
            }
        }
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
            structuredPathIndex[candidatePath]
                .orEmpty()
                .mapTo(resolvedValues) { structuredValue -> structuredValue.toResolvedFieldValue() }

            compatibilityFields[candidatePath]
                ?.let { value -> parseRawFieldValue(value) }
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
            structuredPathIndex.containsKey(candidatePath) || compatibilityFields.containsKey(candidatePath)
        }
    }

    private fun candidatePaths(path: String, isExplicitFieldPath: Boolean): List<String> {
        val aliasBasePaths = if (isExplicitFieldPath) {
            listOf(path)
        } else {
            val loweredPath = path.lowercase()
            CANONICAL_ALIAS_PATHS[loweredPath]?.let { aliases ->
                (listOf(path) + aliases).distinct()
            } ?: listOf(path)
        }

        return aliasBasePaths.flatMap { candidatePath ->
            StructuredQueryPath.parse(candidatePath)
                ?.toLookupCandidates()
                ?: listOf(candidatePath)
        }.distinct()
    }

    private fun StructuredValue.toResolvedFieldValue(): ResolvedFieldValue {
        return when (this) {
            is StructuredValue.StringValue -> ResolvedFieldValue.StringValue(value)
            is StructuredValue.NumberValue -> {
                if (NUMERIC_LITERAL_PATTERN.matches(value)) {
                    runCatching { ResolvedFieldValue.NumberValue(BigDecimal(value)) }
                        .getOrElse { ResolvedFieldValue.StringValue(value) }
                } else {
                    ResolvedFieldValue.StringValue(value)
                }
            }

            is StructuredValue.BooleanValue -> ResolvedFieldValue.BooleanValue(value)
            StructuredValue.NullValue -> ResolvedFieldValue.NullValue
            is StructuredValue.ObjectValue -> ResolvedFieldValue.StringValue(asDisplayString())
            is StructuredValue.ArrayValue -> ResolvedFieldValue.StringValue(asDisplayString())
        }
    }

    private fun parseRawFieldValue(value: String): ResolvedFieldValue {
        val normalized = value.trim()
        if (normalized.equals("true", ignoreCase = true)) {
            return ResolvedFieldValue.BooleanValue(true)
        }
        if (normalized.equals("false", ignoreCase = true)) {
            return ResolvedFieldValue.BooleanValue(false)
        }
        if (normalized.equals("null", ignoreCase = true)) {
            return ResolvedFieldValue.NullValue
        }
        if (NUMERIC_LITERAL_PATTERN.matches(normalized)) {
            return runCatching { ResolvedFieldValue.NumberValue(BigDecimal(normalized)) }
                .getOrElse { ResolvedFieldValue.StringValue(value) }
        }
        return ResolvedFieldValue.StringValue(value)
    }

    private fun QueryLiteral?.toComparableNumber(): BigDecimal? {
        return when (this) {
            is QueryLiteral.NumberValue -> value
            is QueryLiteral.StringValue -> runCatching { BigDecimal(value) }.getOrNull()
            else -> null
        }
    }

    private fun QueryLiteral.toSearchToken(): String {
        return when (this) {
            is QueryLiteral.StringValue -> value
            is QueryLiteral.NumberValue -> value.toPlainString()
            is QueryLiteral.BooleanValue -> value.toString()
            QueryLiteral.NullValue -> "null"
        }
    }

    private fun ResolvedFieldValue.toComparableNumber(): BigDecimal? {
        return when (this) {
            is ResolvedFieldValue.NumberValue -> value
            is ResolvedFieldValue.StringValue -> runCatching { BigDecimal(value) }.getOrNull()
            else -> null
        }
    }

    private fun ResolvedFieldValue.toComparableBoolean(): Boolean? {
        return when (this) {
            is ResolvedFieldValue.BooleanValue -> value
            is ResolvedFieldValue.StringValue -> {
                when {
                    value.equals("true", ignoreCase = true) -> true
                    value.equals("false", ignoreCase = true) -> false
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun ResolvedFieldValue.toSearchToken(): String {
        return when (this) {
            is ResolvedFieldValue.StringValue -> value
            is ResolvedFieldValue.NumberValue -> value.toPlainString()
            is ResolvedFieldValue.BooleanValue -> value.toString()
            ResolvedFieldValue.NullValue -> "null"
        }
    }

    private fun lowerBoundWithTolerance(bound: Instant): Instant {
        return runCatching { bound.minusSeconds(TIME_FILTER_TOLERANCE_SECONDS) }
            .getOrElse { bound }
    }

    private fun upperBoundWithTolerance(bound: Instant): Instant {
        return runCatching { bound.plusSeconds(TIME_FILTER_TOLERANCE_SECONDS) }
            .getOrElse { bound }
    }

    private sealed interface ResolvedFieldValue {
        data class StringValue(val value: String) : ResolvedFieldValue
        data class NumberValue(val value: BigDecimal) : ResolvedFieldValue
        data class BooleanValue(val value: Boolean) : ResolvedFieldValue
        data object NullValue : ResolvedFieldValue
    }
}
