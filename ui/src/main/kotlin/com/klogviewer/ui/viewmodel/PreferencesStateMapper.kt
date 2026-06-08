package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.*

object PreferencesStateMapper {
    fun toState(prefs: UserPreferences): KLogViewerState {
        if (prefs.tabs.isEmpty()) {
            return baseState(prefs = prefs, tabs = null)
        }

        return baseState(
            prefs = prefs,
            tabs = prefs.tabs.map(::toTabState),
            activeTabId = prefs.activeTabId ?: prefs.tabs.firstOrNull()?.id
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
            tabs = state.tabs.map(::toTabPreference),
            activeTabId = state.activeTabId
        )
    }

    private fun baseState(
        prefs: UserPreferences,
        tabs: List<TabState>?,
        activeTabId: String? = null
    ): KLogViewerState {
        return if (tabs == null) {
            KLogViewerState(
                isDarkMode = prefs.isDarkMode,
                isSidebarExpanded = prefs.isSidebarExpanded,
                recentFiles = prefs.recentFiles,
                recentDirectories = prefs.recentDirectories,
                sftpConnections = prefs.sftpConnections,
                s3Connections = prefs.s3Connections
            )
        } else {
            KLogViewerState(
                tabs = tabs,
                activeTabId = activeTabId,
                isDarkMode = prefs.isDarkMode,
                isSidebarExpanded = prefs.isSidebarExpanded,
                recentFiles = prefs.recentFiles,
                recentDirectories = prefs.recentDirectories,
                sftpConnections = prefs.sftpConnections,
                s3Connections = prefs.s3Connections
            )
        }
    }

    private fun toTabState(tabPreference: TabPreference): TabState {
        return TabState(
            id = tabPreference.id,
            title = tabPreference.title,
            windows = tabPreference.windows.map(::toLogWindow),
            activeWindowId = tabPreference.activeWindowId
        )
    }

    private fun toLogWindow(windowPreference: WindowPreference): LogWindow {
        val fromInstant = TimeRangeFilterSupport.parseInstantOrNull(windowPreference.timeFilterFrom)
        val toInstant = TimeRangeFilterSupport.parseInstantOrNull(windowPreference.timeFilterTo)

        return LogWindow(
            id = windowPreference.id,
            filePath = windowPreference.filePath,
            sourceIds = windowPreference.sourceIds,
            hiddenSourceIds = windowPreference.hiddenSourceIds,
            filterQueries = windowPreference.filterQueries,
            levelFilters = resolveLevelFilters(windowPreference),
            timeFilterFrom = windowPreference.timeFilterFrom,
            timeFilterTo = windowPreference.timeFilterTo,
            timeFilterFromInstant = fromInstant,
            timeFilterToInstant = toInstant,
            timeFilterPreset = TimeRangeFilterSupport.toPreset(windowPreference.timeFilterPresetMinutes),
            timeFilterValidationMessage = TimeRangeFilterSupport.validationMessage(
                windowPreference.timeFilterFrom,
                fromInstant,
                windowPreference.timeFilterTo,
                toInstant
            ),
            isReversed = windowPreference.isReversed,
            isAutoScrollEnabled = windowPreference.isAutoScrollEnabled,
            showAnsiColors = windowPreference.showAnsiColors,
            parserName = windowPreference.parserName,
            columns = windowPreference.columns,
            columnWidths = windowPreference.columnWidths,
            isConnected = windowPreference.isConnected,
            logFontFamily = windowPreference.logFontFamily,
            logFontSizeSp = windowPreference.logFontSizeSp
        )
    }

    private fun resolveLevelFilters(windowPreference: WindowPreference): Set<LevelFilterKey> {
        val typedFilters = LevelFilterPolicy.toTypedFilters(windowPreference.levelFilters)
        return if (typedFilters.isEmpty() && windowPreference.levelFilters.isNotEmpty()) {
            LevelFilterPolicy.defaultFilters
        } else {
            typedFilters
        }
    }

    private fun toTabPreference(tab: TabState): TabPreference {
        return TabPreference(
            id = tab.id,
            title = tab.title,
            activeWindowId = tab.activeWindowId,
            windows = tab.windows.map(::toWindowPreference)
        )
    }

    private fun toWindowPreference(window: LogWindow): WindowPreference {
        return WindowPreference(
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
}
