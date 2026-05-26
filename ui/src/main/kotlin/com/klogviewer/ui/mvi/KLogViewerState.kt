package com.klogviewer.ui.mvi

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig
import java.time.Instant

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
    val isAutoScrollEnabled: Boolean = true,
    val showAnsiColors: Boolean = true,
    val sourceIds: List<String> = emptyList(),
    val missingSourceIds: Set<String> = emptySet(),
    val selectedEntry: LogEntry? = null,
    val selectedIndices: Set<Int> = emptySet(),
    val lastSelectedIndex: Int? = null,
    val parserName: String? = null,
    val columns: List<String> = emptyList(),
    val columnWidths: Map<String, Int> = emptyMap(),
    val isConnected: Boolean = true,
    val isDirectory: Boolean = false,
    val timeFilterFrom: String = "",
    val timeFilterTo: String = "",
    val timeFilterFromInstant: Instant? = null,
    val timeFilterToInstant: Instant? = null,
    val timeFilterPreset: TimeRangePreset? = null,
    val timeFilterValidationMessage: String? = null
) {
    val levelCounts: Map<LogLevel, Int> get() = logs.groupingBy { it.level }.eachCount()
}

enum class TimeRangePreset {
    LAST_5_MINUTES,
    LAST_15_MINUTES,
    LAST_30_MINUTES,
    LAST_1_HOUR,
    LAST_6_HOURS,
    LAST_24_HOURS
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

data class KLogViewerState(
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
    val pendingDialog: DialogType? = null,
    val sftpConnections: List<SftpConfig> = emptyList(),
    val s3Connections: List<S3Config> = emptyList(),
    val remoteFiles: List<com.klogviewer.domain.model.RemoteFile> = emptyList(),
    val isRemoteLoading: Boolean = false,
    val remoteBrowsePath: String = "",
    val currentSftpConfig: SftpConfig? = null,
    val currentS3Config: S3Config? = null,
    val pendingPlaintextSecretSave: PlaintextSecretSavePrompt? = null,
    val isAddMode: Boolean = false
) {
    enum class DialogType { OPEN, OPEN_DIRECTORY, ADD, ADD_DIRECTORY, RECENT_ITEMS, SFTP_CONNECT, SFTP_ADD, SFTP_BROWSE, S3_CONNECT, S3_ADD, S3_BROWSE }
    val activeTab: TabState? get() = tabs.find { it.id == activeTabId }

    fun updateActiveTab(block: (TabState) -> TabState): KLogViewerState {
        return copy(tabs = tabs.map { if (it.id == activeTabId) block(it) else it })
    }

    fun updateActiveWindow(block: (LogWindow) -> LogWindow): KLogViewerState {
        return updateActiveTab { it.updateActiveWindow(block) }
    }

    fun updateWindow(windowId: String, block: (LogWindow) -> LogWindow): KLogViewerState {
        return copy(tabs = tabs.map { tab ->
            tab.copy(windows = tab.windows.map { if (it.id == windowId) block(it) else it })
        })
    }
}

data class PlaintextSecretSavePrompt(
    val title: String,
    val message: String
)
