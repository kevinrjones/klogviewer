package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.LogEntryDetailViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class EntryIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val onCopySelectedToClipboard: () -> Unit,
    private val onCopyStructuredText: (String) -> Unit
) {
    fun handle(intent: KLogViewerIntent.EntryIntent) {
        when (intent) {
            is KLogViewerIntent.SelectEntry -> reduceSelection(intent)
            is KLogViewerIntent.ToggleEntrySelection -> reduceEntryToggle(intent)
            is KLogViewerIntent.SetEntryDetailViewMode -> reduceDetailMode(intent)
            is KLogViewerIntent.ToggleStructuredPathExpansion -> reducePathExpansion(intent)
            is KLogViewerIntent.ToggleStructuredScalarExpansion -> reduceScalarExpansion(intent)
            is KLogViewerIntent.ToggleRawPayloadExpansion -> reduceRawExpansion(intent)
            is KLogViewerIntent.CopyStructuredText -> onCopyStructuredText(intent.text)
            KLogViewerIntent.CopySelected -> copySelectedIfPresent()
        }
    }

    private fun reduceSelection(intent: KLogViewerIntent.SelectEntry) {
        updateActiveWindow { window -> reduceSelectedEntry(window, intent.entry) }
    }

    private fun reduceEntryToggle(intent: KLogViewerIntent.ToggleEntrySelection) {
        updateActiveWindow { window -> reduceToggledSelection(window, intent) }
    }

    private fun reduceDetailMode(intent: KLogViewerIntent.SetEntryDetailViewMode) {
        updateActiveWindow { window -> window.copy(detailViewMode = intent.mode) }
    }

    private fun reducePathExpansion(intent: KLogViewerIntent.ToggleStructuredPathExpansion) {
        updateActiveWindow { window ->
            window.copy(expandedStructuredPaths = togglePath(window.expandedStructuredPaths, intent.path))
        }
    }

    private fun reduceScalarExpansion(intent: KLogViewerIntent.ToggleStructuredScalarExpansion) {
        updateActiveWindow { window ->
            window.copy(expandedStructuredScalarPaths = togglePath(window.expandedStructuredScalarPaths, intent.path))
        }
    }

    private fun reduceRawExpansion(intent: KLogViewerIntent.ToggleRawPayloadExpansion) {
        updateActiveWindow { window -> window.copy(isRawPayloadExpanded = intent.expanded) }
    }

    private fun copySelectedIfPresent() {
        if (state.value.activeTab?.activeWindow?.selectedIndices?.isNotEmpty() == true) {
            onCopySelectedToClipboard()
        }
    }

    private fun updateActiveWindow(reducer: (LogWindow) -> LogWindow) {
        state.update { currentState ->
            currentState.updateActiveWindow { window -> reducer(window) }
        }
    }
}

private fun reduceSelectedEntry(window: LogWindow, entry: LogEntry?): LogWindow {
    val index = window.filteredLogs.indexOf(entry)
    val selectedIndices = if (entry != null && index != -1) {
        setOf(index)
    } else {
        emptySet()
    }
    return window.copy(
        selectedEntry = entry,
        detailViewMode = resolveDetailViewMode(entry),
        expandedStructuredPaths = emptySet(),
        expandedStructuredScalarPaths = emptySet(),
        isRawPayloadExpanded = false,
        selectedIndices = selectedIndices,
        lastSelectedIndex = if (index != -1) index else null
    )
}

private fun reduceToggledSelection(
    window: LogWindow,
    intent: KLogViewerIntent.ToggleEntrySelection
): LogWindow {
    val newIndices = resolveSelectedIndices(window, intent)
    val nextSelectedEntry = if (newIndices.size == 1) {
        window.filteredLogs.getOrNull(intent.index)
    } else {
        window.selectedEntry
    }
    val selectedEntryChanged = nextSelectedEntry != window.selectedEntry

    return window.copy(
        selectedIndices = newIndices,
        lastSelectedIndex = intent.index,
        selectedEntry = nextSelectedEntry,
        detailViewMode = if (selectedEntryChanged) {
            resolveDetailViewMode(nextSelectedEntry)
        } else {
            window.detailViewMode
        },
        expandedStructuredPaths = if (selectedEntryChanged) {
            emptySet()
        } else {
            window.expandedStructuredPaths
        },
        expandedStructuredScalarPaths = if (selectedEntryChanged) {
            emptySet()
        } else {
            window.expandedStructuredScalarPaths
        },
        isRawPayloadExpanded = if (selectedEntryChanged) false else window.isRawPayloadExpanded
    )
}

private fun resolveSelectedIndices(
    window: LogWindow,
    intent: KLogViewerIntent.ToggleEntrySelection
): Set<Int> {
    return when {
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
}

private fun togglePath(expandedPaths: Set<String>, path: String): Set<String> {
    return if (expandedPaths.contains(path)) {
        expandedPaths - path
    } else {
        expandedPaths + path
    }
}

private fun resolveDetailViewMode(entry: LogEntry?): LogEntryDetailViewMode {
    return if (entry?.structuredData != null) {
        LogEntryDetailViewMode.STRUCTURED
    } else {
        LogEntryDetailViewMode.RAW
    }
}
