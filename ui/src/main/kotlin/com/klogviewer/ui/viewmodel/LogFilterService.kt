package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LogFilterService {
    suspend fun filter(window: LogWindow): List<LogEntry> = withContext(Dispatchers.Default) {
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
            val matchesDashboardFilter = window.dashboardFilterQuery?.let { query ->
                entry.timestamp.value.contains(query, ignoreCase = true)
            } ?: true
            matchesLevel && matchesFilter && matchesDashboardFilter
        }
        
        if (window.isReversed) filtered.reversed() else filtered
    }
}
