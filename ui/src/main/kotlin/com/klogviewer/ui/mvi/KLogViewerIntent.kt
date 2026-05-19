package com.klogviewer.ui.mvi

import com.klogviewer.domain.model.LogLevel

sealed interface KLogViewerIntent {
    data class LoadFiles(val paths: List<String>) : KLogViewerIntent
    data class AddToWorkspace(val paths: List<String>) : KLogViewerIntent
    data class SelectPath(val path: String) : KLogViewerIntent
    data object ClearLogs : KLogViewerIntent
    data object ToggleTheme : KLogViewerIntent
    data object ToggleSidebar : KLogViewerIntent
    data class AddFilterQuery(val query: String) : KLogViewerIntent
    data class RemoveFilterQuery(val query: String) : KLogViewerIntent
    data object ClearFilterQueries : KLogViewerIntent
    data class ToggleLevel(val level: LogLevel) : KLogViewerIntent
    data object ToggleAllLevels : KLogViewerIntent
    data object ToggleSortOrder : KLogViewerIntent
    data object ToggleAutoScroll : KLogViewerIntent
    data object ToggleAnsiColors : KLogViewerIntent
    data class SelectEntry(val entry: com.klogviewer.domain.model.LogEntry?) : KLogViewerIntent
    data class ToggleEntrySelection(val index: Int, val isShiftPressed: Boolean = false, val isMetaPressed: Boolean = false) : KLogViewerIntent
    data object CopySelected : KLogViewerIntent
    
    // Dialogs
    data object ShowOpenDialog : KLogViewerIntent
    data object ShowOpenDirectoryDialog : KLogViewerIntent
    data object ShowAddDialog : KLogViewerIntent
    data object ShowRecentDialog : KLogViewerIntent
    data object DismissDialog : KLogViewerIntent
    data class RemoveRecentItem(val path: String) : KLogViewerIntent
    data object ClearMissingRecentItems : KLogViewerIntent
    
    // Tab Management
    data object AddTab : KLogViewerIntent
    data class CloseTab(val id: String) : KLogViewerIntent
    data class SwitchTab(val id: String) : KLogViewerIntent
    
    // Split Management
    data object SplitHorizontal : KLogViewerIntent
    data class CloseWindow(val id: String) : KLogViewerIntent
    data class SwitchWindow(val id: String) : KLogViewerIntent
    data class UpdateColumnWidth(val windowId: String, val column: String, val width: Int) : KLogViewerIntent
}
