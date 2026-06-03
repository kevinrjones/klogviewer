package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

class WorkspaceIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val logLoadingCoordinator: LogLoadingCoordinator,
    private val recentItemsManager: RecentItemsManager,
    private val onSavePreferences: () -> Unit,
    private val onFilterLogs: (String?) -> Unit,
    private val onShowInfo: (String) -> Unit
) {
    fun handle(intent: KLogViewerIntent.WorkspaceIntent) {
        when (intent) {
            is KLogViewerIntent.LoadFiles -> handleLoadFiles(intent)
            is KLogViewerIntent.AddToWorkspace -> handleAddToWorkspace(intent)
            is KLogViewerIntent.DropFilesOnLogView -> handleDropFilesOnLogView(intent)
            is KLogViewerIntent.DropFilesOnTabBar -> handleDropFilesOnTabBar(intent)
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

    private fun handleDropFilesOnLogView(intent: KLogViewerIntent.DropFilesOnLogView) {
        handleValidatedDrop(intent.paths) { validPaths ->
            handleAddToWorkspace(KLogViewerIntent.AddToWorkspace(validPaths))
        }
    }

    private fun handleDropFilesOnTabBar(intent: KLogViewerIntent.DropFilesOnTabBar) {
        handleValidatedDrop(intent.paths) { validPaths ->
            state.update { TabWindowController.addTab(it) }
            onSavePreferences()
            handleLoadFiles(KLogViewerIntent.LoadFiles(validPaths))
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
                window.copy(
                    logs = emptyList(),
                    filePath = "",
                    sourceIds = emptyList(),
                    hiddenSourceIds = emptySet()
                )
            }
        }
        onFilterLogs(activeWindow.id)
        onSavePreferences()
    }

    private fun handleValidatedDrop(paths: List<String>, onValidDrop: (List<String>) -> Unit) {
        val validPaths = paths.distinct().filter(::isSupportedDropPath)
        val invalidCount = paths.size - validPaths.size

        when {
            validPaths.isEmpty() -> onShowInfo("Dropped items are not supported.")
            else -> {
                onValidDrop(validPaths)
                if (invalidCount > 0) {
                    onShowInfo("Ignored $invalidCount unsupported dropped item(s).")
                }
            }
        }
    }

    private fun isSupportedDropPath(path: String): Boolean {
        if (path.isBlank()) {
            return false
        }
        if (path.startsWith("sftp://") || path.startsWith("s3://")) {
            return true
        }
        return File(path).exists()
    }
}
