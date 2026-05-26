package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LogFilterService {
    suspend fun filter(window: LogWindow): List<LogEntry> = withContext(Dispatchers.Default) {
        val timeRange = TimeRangeFilterSupport.resolveRange(window)
        val filtered = window.logs.filter { entry ->
            val matchesLevel = window.levelFilters.contains(entry.level)
            val matchesFilter = if (window.filterQueries.isEmpty()) {
                true
            } else {
                window.filterQueries.all { query ->
                    entry.content.value.contains(query, ignoreCase = true) ||
                    entry.timestamp.value.contains(query, ignoreCase = true)
                }
            }
            val matchesTimeRange = timeRange?.let { (from, to) ->
                val entryInstant = TimeRangeFilterSupport.entryInstant(entry) ?: return@let false
                !entryInstant.isBefore(from) && !entryInstant.isAfter(to)
            } ?: true
            matchesLevel && matchesFilter && matchesTimeRange
        }

        if (window.isReversed) filtered.reversed() else filtered
    }
}
