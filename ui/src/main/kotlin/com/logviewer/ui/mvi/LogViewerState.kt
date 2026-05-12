package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel

data class LogViewerState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filePath: String = "",
    val isDarkMode: Boolean = true,
    val isSidebarExpanded: Boolean = true,
    val searchQuery: String = "",
    val levelFilters: Set<LogLevel> = LogLevel.entries.toSet()
)
