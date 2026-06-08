package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

class EntryIntentHandlerTest {

    @Test
    fun `given selected index when shift selecting later index then full range is selected`() {
        val logs = listOf(logEntry("line-1"), logEntry("line-2"), logEntry("line-3"), logEntry("line-4"))
        val state = MutableStateFlow(stateWithLogs(logs))
        val handler = EntryIntentHandler(state = state, onCopySelectedToClipboard = {}, onCopyStructuredText = {})

        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 1))
        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 3, isShiftPressed = true))

        val selectedIndices = state.value.activeTab?.activeWindow?.selectedIndices.orEmpty()
        expectThat(selectedIndices).isEqualTo(setOf(1, 2, 3))
    }

    @Test
    fun `given selection when meta toggling selected index then index is removed`() {
        val logs = listOf(logEntry("line-1"), logEntry("line-2"), logEntry("line-3"))
        val state = MutableStateFlow(stateWithLogs(logs))
        val handler = EntryIntentHandler(state = state, onCopySelectedToClipboard = {}, onCopyStructuredText = {})

        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 0))
        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 2, isMetaPressed = true))
        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 0, isMetaPressed = true))

        val selectedIndices = state.value.activeTab?.activeWindow?.selectedIndices.orEmpty()
        expectThat(selectedIndices).isEqualTo(setOf(2))
    }

    @Test
    fun `given empty selection when copy is requested then callback is not invoked`() {
        val logs = listOf(logEntry("line-1"), logEntry("line-2"))
        val state = MutableStateFlow(stateWithLogs(logs))
        var copyInvocations = 0
        val handler = EntryIntentHandler(
            state = state,
            onCopySelectedToClipboard = { copyInvocations += 1 },
            onCopyStructuredText = {}
        )

        handler.handle(KLogViewerIntent.CopySelected)

        expectThat(copyInvocations).isEqualTo(0)
    }

    @Test
    fun `given non empty selection when copy is requested then callback is invoked`() {
        val logs = listOf(logEntry("line-1"), logEntry("line-2"))
        val state = MutableStateFlow(stateWithLogs(logs))
        var copyInvocations = 0
        val handler = EntryIntentHandler(
            state = state,
            onCopySelectedToClipboard = { copyInvocations += 1 },
            onCopyStructuredText = {}
        )

        handler.handle(KLogViewerIntent.ToggleEntrySelection(index = 1))
        handler.handle(KLogViewerIntent.CopySelected)

        expectThat(state.value.activeTab?.activeWindow?.selectedIndices.orEmpty()).contains(1)
        expectThat(copyInvocations).isEqualTo(1)
    }

    private fun stateWithLogs(logs: List<LogEntry>): KLogViewerState = KLogViewerState(
        tabs = listOf(
            TabState(
                id = "tab-1",
                title = "Log View",
                windows = listOf(
                    LogWindow(
                        id = "window-1",
                        logs = logs,
                        filteredLogs = logs
                    )
                ),
                activeWindowId = "window-1"
            )
        ),
        activeTabId = "tab-1"
    )

    private fun logEntry(content: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("2026-06-02T09:33:00Z"),
        level = LogLevel.INFO,
        content = LogContent(content)
    )
}
