package com.logviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val windowState: WindowStatePreferences = WindowStatePreferences(),
    val recentFiles: List<String> = emptyList(),
    val recentDirectories: List<String> = emptyList(),
    val isDarkMode: Boolean = true,
    val isSidebarExpanded: Boolean = true
)

@Serializable
data class WindowStatePreferences(
    val width: Int = 1200,
    val height: Int = 800,
    val x: Int? = null,
    val y: Int? = null,
    val isMaximized: Boolean = false
)
