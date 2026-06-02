package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class EntryIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val onCopySelectedToClipboard: () -> Unit
) {
    fun handle(intent: KLogViewerIntent.EntryIntent) {
        when (intent) {
            is KLogViewerIntent.SelectEntry -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val index = window.filteredLogs.indexOf(intent.entry)
                        window.copy(
                            selectedEntry = intent.entry,
                            selectedIndices = if (intent.entry != null && index != -1) setOf(index) else emptySet(),
                            lastSelectedIndex = if (index != -1) index else null
                        )
                    }
                }
            }
            is KLogViewerIntent.ToggleEntrySelection -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val newIndices = when {
                            intent.isShiftPressed && window.lastSelectedIndex != null -> {
                                val start = minOf(window.lastSelectedIndex, intent.index)
                                val end = maxOf(window.lastSelectedIndex, intent.index)
                                window.selectedIndices + (start..end).toSet()
                            }
                            intent.isMetaPressed -> {
                                if (window.selectedIndices.contains(intent.index)) {
                                    window.selectedIndices - intent.index
                                } else {
                                    window.selectedIndices + intent.index
                                }
                            }
                            else -> setOf(intent.index)
                        }
                        window.copy(
                            selectedIndices = newIndices,
                            lastSelectedIndex = intent.index,
                            selectedEntry = if (newIndices.size == 1) window.filteredLogs.getOrNull(intent.index) else window.selectedEntry
                        )
                    }
                }
            }
            KLogViewerIntent.CopySelected -> {
                val hasSelection = state.value.activeTab?.activeWindow?.selectedIndices?.isNotEmpty() == true
                if (hasSelection) {
                    onCopySelectedToClipboard()
                }
            }
        }
    }
}
