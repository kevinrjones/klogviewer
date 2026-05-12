package com.logviewer.ui.mvi

sealed interface LogViewerIntent {
    data class LoadFile(val path: String) : LogViewerIntent
    data class SelectPath(val path: String) : LogViewerIntent
    data object ClearLogs : LogViewerIntent
}
