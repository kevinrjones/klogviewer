package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TabWindowIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val logLoadingCoordinator: LogLoadingCoordinator,
    private val onSavePreferences: (debounce: Boolean) -> Unit
) {
    fun handle(intent: KLogViewerIntent.TabWindowIntent) {
        when (intent) {
            KLogViewerIntent.AddTab -> {
                state.update { TabWindowController.addTab(it) }
                onSavePreferences(false)
            }
            is KLogViewerIntent.CloseTab -> {
                state.update { TabWindowController.closeTab(it, intent.id) { windowId ->
                    logLoadingCoordinator.cancelWindowJob(windowId)
                } }
                onSavePreferences(false)
            }
            is KLogViewerIntent.SwitchTab -> {
                state.update { it.copy(activeTabId = intent.id) }
                onSavePreferences(false)
            }
            KLogViewerIntent.SplitHorizontal -> {
                state.update { TabWindowController.splitHorizontal(it) }
                onSavePreferences(false)
            }
            is KLogViewerIntent.CloseWindow -> {
                state.update { TabWindowController.closeWindow(it, intent.id) { windowId ->
                    logLoadingCoordinator.cancelWindowJob(windowId)
                } }
                onSavePreferences(false)
            }
            is KLogViewerIntent.SwitchWindow -> {
                state.update { TabWindowController.switchWindow(it, intent.id) }
                onSavePreferences(false)
            }
            is KLogViewerIntent.UpdateColumnWidth -> {
                state.update { currentState ->
                    currentState.updateWindow(intent.windowId) { window ->
                        window.copy(columnWidths = window.columnWidths + (intent.column to intent.width))
                    }
                }
                onSavePreferences(true)
            }
            is KLogViewerIntent.ChangeParser -> {
                val window = state.value.tabs.flatMap { it.windows }.find { it.id == intent.windowId }
                if (window != null && window.sourceIds.isNotEmpty()) {
                    logLoadingCoordinator.loadFilesIntoWindow(intent.windowId, window.sourceIds, intent.parserName)
                }
            }
        }
    }
}
