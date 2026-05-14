package com.logviewer.ui.mvi

import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel

data class TabState(
    val id: String,
    val title: String,
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filePath: String = "",
    val searchQuery: String = "",
    val levelFilters: Set<LogLevel> = LogLevel.entries.toSet(),
    val sourceIds: List<String> = emptyList(),
    val selectedEntry: LogEntry? = null
)

data class LogViewerState(
    val tabs: List<TabState> = listOf(TabState(id = "default", title = "Log View")),
    val activeTabId: String? = "default",
    val isDarkMode: Boolean = true,
    val isSidebarExpanded: Boolean = true,
    val pendingDialog: DialogType? = null
) {
    enum class DialogType { OPEN, ADD }
    val activeTab: TabState? get() = tabs.find { it.id == activeTabId }

    fun updateActiveTab(block: (TabState) -> TabState): LogViewerState {
        return copy(tabs = tabs.map { if (it.id == activeTabId) block(it) else it })
    }
}
