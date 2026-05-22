package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.*

object PreferencesStateMapper {
    fun toState(prefs: UserPreferences): KLogViewerState {
        if (prefs.tabs.isEmpty()) {
            return KLogViewerState(
                isDarkMode = prefs.isDarkMode,
                isSidebarExpanded = prefs.isSidebarExpanded,
                recentFiles = prefs.recentFiles,
                recentDirectories = prefs.recentDirectories,
                sftpConnections = prefs.sftpConnections
            )
        }

        return KLogViewerState(
            tabs = prefs.tabs.map { tp ->
                TabState(
                    id = tp.id,
                    title = tp.title,
                    windows = tp.windows.map { wp ->
                        LogWindow(
                            id = wp.id,
                            filePath = wp.filePath,
                            sourceIds = wp.sourceIds,
                            filterQueries = wp.filterQueries,
                            levelFilters = wp.levelFilters,
                            isReversed = wp.isReversed,
                            isAutoScrollEnabled = wp.isAutoScrollEnabled,
                            showAnsiColors = wp.showAnsiColors,
                            parserName = wp.parserName,
                            columns = wp.columns,
                            columnWidths = wp.columnWidths,
                            isConnected = wp.isConnected
                        )
                    },
                    activeWindowId = tp.activeWindowId
                )
            },
            activeTabId = prefs.activeTabId ?: prefs.tabs.firstOrNull()?.id,
            isDarkMode = prefs.isDarkMode,
            isSidebarExpanded = prefs.isSidebarExpanded,
            recentFiles = prefs.recentFiles,
            recentDirectories = prefs.recentDirectories,
            sftpConnections = prefs.sftpConnections
        )
    }

    fun toPreferences(state: KLogViewerState, currentPrefs: UserPreferences): UserPreferences {
        return currentPrefs.copy(
            isDarkMode = state.isDarkMode,
            isSidebarExpanded = state.isSidebarExpanded,
            recentFiles = state.recentFiles,
            recentDirectories = state.recentDirectories,
            sftpConnections = state.sftpConnections,
            tabs = state.tabs.map { tab ->
                TabPreference(
                    id = tab.id,
                    title = tab.title,
                    activeWindowId = tab.activeWindowId,
                    windows = tab.windows.map { window ->
                        WindowPreference(
                            id = window.id,
                            filePath = window.filePath,
                            sourceIds = window.sourceIds,
                            filterQueries = window.filterQueries,
                            levelFilters = window.levelFilters,
                            isReversed = window.isReversed,
                            isAutoScrollEnabled = window.isAutoScrollEnabled,
                            showAnsiColors = window.showAnsiColors,
                            parserName = window.parserName,
                            columns = window.columns,
                            columnWidths = window.columnWidths,
                            isConnected = window.isConnected
                        )
                    }
                )
            },
            activeTabId = state.activeTabId
        )
    }
}
