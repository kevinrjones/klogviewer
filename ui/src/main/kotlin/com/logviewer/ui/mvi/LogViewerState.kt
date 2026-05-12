package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogEntry

data class LogViewerState(
    val logs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filePath: String = ""
)
