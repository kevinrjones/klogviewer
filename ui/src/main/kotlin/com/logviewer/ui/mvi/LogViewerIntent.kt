package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogLevel

sealed interface LogViewerIntent {
    data class LoadFiles(val paths: List<String>) : LogViewerIntent
    data class AddToWorkspace(val paths: List<String>) : LogViewerIntent
    data class SelectPath(val path: String) : LogViewerIntent
    data object ClearLogs : LogViewerIntent
    data object ToggleTheme : LogViewerIntent
    data object ToggleSidebar : LogViewerIntent
    data class UpdateSearch(val query: String) : LogViewerIntent
    data class ToggleLevel(val level: LogLevel) : LogViewerIntent
    data class SelectEntry(val entry: com.logviewer.domain.model.LogEntry?) : LogViewerIntent
    
    // Dialogs
    data object ShowOpenDialog : LogViewerIntent
    data object ShowAddDialog : LogViewerIntent
    data object DismissDialog : LogViewerIntent
    
    // Tab Management
    data object AddTab : LogViewerIntent
    data class CloseTab(val id: String) : LogViewerIntent
    data class SwitchTab(val id: String) : LogViewerIntent
}
