package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogLevel

sealed interface LogViewerIntent {
    data class LoadFile(val path: String) : LogViewerIntent
    data class SelectPath(val path: String) : LogViewerIntent
    data object ClearLogs : LogViewerIntent
    data object ToggleTheme : LogViewerIntent
    data object ToggleSidebar : LogViewerIntent
    data class UpdateSearch(val query: String) : LogViewerIntent
    data class ToggleLevel(val level: LogLevel) : LogViewerIntent
}
