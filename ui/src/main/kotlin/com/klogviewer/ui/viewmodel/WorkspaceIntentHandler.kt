package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class WorkspaceIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val logLoadingCoordinator: LogLoadingCoordinator,
    private val recentItemsManager: RecentItemsManager,
    private val onSavePreferences: () -> Unit,
    private val onFilterLogs: (String?) -> Unit
) {
    fun handle(intent: KLogViewerIntent.WorkspaceIntent) {
        when (intent) {
            is KLogViewerIntent.LoadFiles -> handleLoadFiles(intent)
            is KLogViewerIntent.AddToWorkspace -> handleAddToWorkspace(intent)
            is KLogViewerIntent.SelectPath -> handleSelectPath(intent)
            KLogViewerIntent.ClearLogs -> handleClearLogs()
        }
    }

    private fun handleLoadFiles(intent: KLogViewerIntent.LoadFiles) {
        val activeWindowId = state.value.activeTab?.activeWindow?.id
        if (activeWindowId != null) {
            logLoadingCoordinator.loadFilesIntoWindow(activeWindowId, intent.paths)
            state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
        }
    }

    private fun handleAddToWorkspace(intent: KLogViewerIntent.AddToWorkspace) {
        val activeWindow = state.value.activeTab?.activeWindow
        if (activeWindow != null) {
            val currentPaths = activeWindow.sourceIds
            val allPaths = (currentPaths + intent.paths).distinct()
            logLoadingCoordinator.loadFilesIntoWindow(activeWindow.id, allPaths)
            state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
        }
    }

    private fun handleSelectPath(intent: KLogViewerIntent.SelectPath) {
        state.update { it.updateActiveWindow { window -> window.copy(filePath = intent.path) } }
        onSavePreferences()
    }

    private fun handleClearLogs() {
        val activeWindow = state.value.activeTab?.activeWindow ?: return
        logLoadingCoordinator.cancelWindowJob(activeWindow.id)
        state.update { currentState ->
            currentState.updateActiveWindow { window ->
                window.copy(logs = emptyList(), filePath = "", sourceIds = emptyList())
            }
        }
        onFilterLogs(activeWindow.id)
        onSavePreferences()
    }
}
