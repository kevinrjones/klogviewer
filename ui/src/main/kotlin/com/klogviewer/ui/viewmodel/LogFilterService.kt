package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LogFilterService {
    suspend fun filter(window: LogWindow): LogWindow = withContext(Dispatchers.Default) {
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
            matchesLevel && matchesFilter
        }
        
        val sorted = if (window.isReversed) filtered.reversed() else filtered
        window.copy(filteredLogs = sorted)
    }
}
