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
                sftpConnections = prefs.sftpConnections,
                s3Connections = prefs.s3Connections
            )
        }

        return KLogViewerState(
            tabs = prefs.tabs.map { tp ->
                TabState(
                    id = tp.id,
                    title = tp.title,
                    windows = tp.windows.map { wp ->
                        val fromInstant = TimeRangeFilterSupport.parseInstantOrNull(wp.timeFilterFrom)
                        val toInstant = TimeRangeFilterSupport.parseInstantOrNull(wp.timeFilterTo)
                        val levelFilters = LevelFilterPolicy.toTypedFilters(wp.levelFilters).let { typed ->
                            if (typed.isEmpty() && wp.levelFilters.isNotEmpty()) {
                                LevelFilterPolicy.defaultFilters
                            } else {
                                typed
                            }
                        }
                        LogWindow(
                            id = wp.id,
                            filePath = wp.filePath,
                            sourceIds = wp.sourceIds,
                            hiddenSourceIds = wp.hiddenSourceIds,
                            filterQueries = wp.filterQueries,
                            levelFilters = levelFilters,
                            timeFilterFrom = wp.timeFilterFrom,
                            timeFilterTo = wp.timeFilterTo,
                            timeFilterFromInstant = fromInstant,
                            timeFilterToInstant = toInstant,
                            timeFilterPreset = TimeRangeFilterSupport.toPreset(wp.timeFilterPresetMinutes),
                            timeFilterValidationMessage = TimeRangeFilterSupport.validationMessage(
                                wp.timeFilterFrom,
                                fromInstant,
                                wp.timeFilterTo,
                                toInstant
                            ),
                            isReversed = wp.isReversed,
                            isAutoScrollEnabled = wp.isAutoScrollEnabled,
                            showAnsiColors = wp.showAnsiColors,
                            parserName = wp.parserName,
                            columns = wp.columns,
                            columnWidths = wp.columnWidths,
                            isConnected = wp.isConnected,
                            logFontFamily = wp.logFontFamily,
                            logFontSizeSp = wp.logFontSizeSp
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
            sftpConnections = prefs.sftpConnections,
            s3Connections = prefs.s3Connections
        )
    }

    fun toPreferences(state: KLogViewerState, currentPrefs: UserPreferences): UserPreferences {
        return currentPrefs.copy(
            isDarkMode = state.isDarkMode,
            isSidebarExpanded = state.isSidebarExpanded,
            recentFiles = state.recentFiles,
            recentDirectories = state.recentDirectories,
            sftpConnections = state.sftpConnections,
            s3Connections = state.s3Connections,
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
                            hiddenSourceIds = window.hiddenSourceIds,
                            filterQueries = window.filterQueries,
                            levelFilters = LevelFilterPolicy.toRawFilters(window.levelFilters),
                            timeFilterFrom = window.timeFilterFrom,
                            timeFilterTo = window.timeFilterTo,
                            timeFilterPresetMinutes = TimeRangeFilterSupport.toMinutes(window.timeFilterPreset),
                            isReversed = window.isReversed,
                            isAutoScrollEnabled = window.isAutoScrollEnabled,
                            showAnsiColors = window.showAnsiColors,
                            parserName = window.parserName,
                            columns = window.columns,
                            columnWidths = window.columnWidths,
                            isConnected = window.isConnected,
                            logFontFamily = window.logFontFamily,
                            logFontSizeSp = window.logFontSizeSp
                        )
                    }
                )
            },
            activeTabId = state.activeTabId
        )
    }
}
