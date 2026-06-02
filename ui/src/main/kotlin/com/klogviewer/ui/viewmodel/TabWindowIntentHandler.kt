package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TabWindowIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val logLoadingCoordinator: LogLoadingCoordinator,
    private val onSavePreferences: (debounce: Boolean) -> Unit,
    private val onFilterLogs: (windowId: String) -> Unit
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
            is KLogViewerIntent.ToggleSourceVisibilityInActiveWindow -> {
                val activeWindowId = state.value.activeTab?.activeWindow?.id
                state.update { currentState ->
                    currentState.updateWindow(activeWindowId ?: return@update currentState) { window ->
                        if (!window.sourceIds.contains(intent.sourcePath)) {
                            window
                        } else {
                            val updatedHiddenSources = if (window.hiddenSourceIds.contains(intent.sourcePath)) {
                                window.hiddenSourceIds - intent.sourcePath
                            } else {
                                window.hiddenSourceIds + intent.sourcePath
                            }
                            window.copy(
                                hiddenSourceIds = updatedHiddenSources,
                                selectedEntry = window.selectedEntry?.takeIf { selected ->
                                    selected.sourceId == null || !updatedHiddenSources.contains(selected.sourceId)
                                },
                                selectedIndices = emptySet(),
                                lastSelectedIndex = null
                            )
                        }
                    }
                }
                if (activeWindowId != null) {
                    onFilterLogs(activeWindowId)
                }
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
