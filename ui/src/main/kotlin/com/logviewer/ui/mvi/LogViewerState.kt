package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel

data class LogWindow(
    val id: String,
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filePath: String = "",
    val filterQueries: List<String> = emptyList(),
    val levelFilters: Set<LogLevel> = LogLevel.entries.toSet(),
    val isReversed: Boolean = false,
    val sourceIds: List<String> = emptyList(),
    val selectedEntry: LogEntry? = null,
    val columns: List<String> = emptyList(),
    val columnWidths: Map<String, Int> = emptyMap()
) {
    val levelCounts: Map<LogLevel, Int> get() = logs.groupingBy { it.level }.eachCount()
}

data class TabState(
    val id: String,
    val title: String,
    val windows: List<LogWindow> = emptyList(),
    val activeWindowId: String? = null
) {
    val activeWindow: LogWindow? get() = windows.find { it.id == activeWindowId } ?: windows.firstOrNull()

    fun updateActiveWindow(block: (LogWindow) -> LogWindow): TabState {
        val windowId = activeWindowId ?: windows.firstOrNull()?.id ?: return this
        return copy(windows = windows.map { if (it.id == windowId) block(it) else it })
    }
}

data class LogViewerState(
    val tabs: List<TabState> = listOf(
        TabState(
            id = "default",
            title = "Log View",
            windows = listOf(LogWindow(id = "default-window")),
            activeWindowId = "default-window"
        )
    ),
    val activeTabId: String? = "default",
    val isDarkMode: Boolean = true,
    val isSidebarExpanded: Boolean = true,
    val recentFiles: List<String> = emptyList(),
    val recentDirectories: List<String> = emptyList(),
    val pendingDialog: DialogType? = null
) {
    enum class DialogType { OPEN, ADD, RECENT_ITEMS }
    val activeTab: TabState? get() = tabs.find { it.id == activeTabId }

    fun updateActiveTab(block: (TabState) -> TabState): LogViewerState {
        return copy(tabs = tabs.map { if (it.id == activeTabId) block(it) else it })
    }

    fun updateActiveWindow(block: (LogWindow) -> LogWindow): LogViewerState {
        return updateActiveTab { it.updateActiveWindow(block) }
    }
}
