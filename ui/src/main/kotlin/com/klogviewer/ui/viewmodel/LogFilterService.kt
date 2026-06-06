package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object LogFilterService {
    private const val DASHBOARD_FIELD_QUERY_PREFIX = "@field:"
    private const val MISSING_BUCKET_VALUE = "(missing)"
    private const val TIME_FILTER_TOLERANCE_SECONDS = 1L

    suspend fun filter(window: LogWindow): List<LogEntry> = withContext(Dispatchers.Default) {
        val timeRange = TimeRangeFilterSupport.resolveRange(window)
        val filtered = window.logs.filter { entry ->
            val isHiddenSource = entry.sourceId != null && window.hiddenSourceIds.contains(entry.sourceId)
            val matchesLevel = window.levelFilters.contains(entry.resolvedLevelKey())
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
        if (query.startsWith(DASHBOARD_FIELD_QUERY_PREFIX)) {
            val payload = query.removePrefix(DASHBOARD_FIELD_QUERY_PREFIX)
            val delimiterIndex = payload.indexOf('=')
            if (delimiterIndex <= 0 || delimiterIndex == payload.lastIndex) {
                return false
            }

            val key = payload.substring(0, delimiterIndex)
            val value = payload.substring(delimiterIndex + 1)
            val fieldValue = entry.fields[key] ?: MISSING_BUCKET_VALUE
            return fieldValue.contains(value, ignoreCase = true)
        }

        return entry.content.value.contains(query, ignoreCase = true) ||
            entry.timestamp.value.contains(query, ignoreCase = true)
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
