package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class UiToggleIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val logLoadingCoordinator: LogLoadingCoordinator,
    private val onSavePreferences: () -> Unit,
    private val onFilterLogs: (String?) -> Unit
) {
    fun handle(intent: KLogViewerIntent.UiToggleIntent) {
        when (intent) {
            KLogViewerIntent.ToggleTheme -> {
                state.update { it.copy(isDarkMode = !it.isDarkMode) }
                onSavePreferences()
            }
            KLogViewerIntent.ToggleSidebar -> {
                state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
                onSavePreferences()
            }
            KLogViewerIntent.ToggleSortOrder -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(isReversed = !it.isReversed) }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ToggleAutoScroll -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(isAutoScrollEnabled = !it.isAutoScrollEnabled) }
                }
                onSavePreferences()
            }
            KLogViewerIntent.ToggleAnsiColors -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(showAnsiColors = !it.showAnsiColors) }
                }
                onSavePreferences()
            }
            KLogViewerIntent.ToggleConnection -> toggleConnection()
        }
    }

    private fun toggleConnection() {
        val activeWindow = state.value.activeTab?.activeWindow ?: return
        val newConnected = !activeWindow.isConnected
        
        state.update { currentState ->
            currentState.updateActiveWindow { it.copy(isConnected = newConnected) }
        }
        
        if (newConnected) {
            if (activeWindow.sourceIds.isNotEmpty()) {
                logLoadingCoordinator.loadFilesIntoWindow(activeWindow.id, activeWindow.sourceIds, activeWindow.parserName)
            }
        } else {
            logLoadingCoordinator.cancelWindowJob(activeWindow.id)
        }
        
        onSavePreferences()
    }
}
