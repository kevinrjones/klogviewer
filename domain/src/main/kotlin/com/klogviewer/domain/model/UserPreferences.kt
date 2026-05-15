package com.klogviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val windowState: WindowStatePreferences = WindowStatePreferences(),
    val recentFiles: List<String> = emptyList(),
    val recentDirectories: List<String> = emptyList(),
    val isDarkMode: Boolean = true,
    val isSidebarExpanded: Boolean = true,
    val tabs: List<TabPreference> = emptyList(),
    val activeTabId: String? = null
)

@Serializable
data class TabPreference(
    val id: String,
    val title: String,
    val windows: List<WindowPreference>,
    val activeWindowId: String?
)

@Serializable
data class WindowPreference(
    val id: String,
    val filePath: String,
    val sourceIds: List<String> = emptyList(),
    val filterQueries: List<String> = emptyList(),
    val levelFilters: Set<LogLevel> = LogLevel.entries.toSet(),
    val isReversed: Boolean = false,
    val isAutoScrollEnabled: Boolean = true,
    val columns: List<String> = emptyList(),
    val columnWidths: Map<String, Int> = emptyMap()
)

@Serializable
data class WindowStatePreferences(
    val width: Int = 1200,
    val height: Int = 800,
    val x: Int? = null,
    val y: Int? = null,
    val isMaximized: Boolean = false
)
