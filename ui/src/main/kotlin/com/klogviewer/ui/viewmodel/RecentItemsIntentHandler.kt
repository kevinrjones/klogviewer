package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class RecentItemsIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val recentItemsManager: RecentItemsManager,
    private val onSavePreferences: () -> Unit
) {
    fun handle(intent: KLogViewerIntent.RecentItemsIntent) {
        when (intent) {
            is KLogViewerIntent.RemoveRecentItem -> {
                state.update { recentItemsManager.removeRecentItem(it, intent.path) }
                onSavePreferences()
            }
            KLogViewerIntent.ClearMissingRecentItems -> {
                state.update { recentItemsManager.clearMissingRecentItems(it) }
                onSavePreferences()
            }
        }
    }
}
