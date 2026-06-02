package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.repository.Clipboard
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CopySelectionClipboardTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var prefsRepo: JsonPreferencesRepository
    private lateinit var clipboard: Clipboard
    private lateinit var viewModel: KLogViewerViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        prefsRepo = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        clipboard = mockk(relaxed = true)

        viewModel = KLogViewerViewModel(
            logSource = mockk<LogSource>(relaxed = true),
            prefsRepository = prefsRepo,
            heuristicProbe = HeuristicProbe(ParserRegistry()),
            clipboard = clipboard
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.clear()
    }

    @Test
    fun `given selected indices when copy selected then clipboard receives lines in visible order`() {
        val logs = listOf(logEntry("line-1"), logEntry("line-2"), logEntry("line-3"))
        seedWindowState(logs = logs, selectedIndices = setOf(2, 0))
        viewModel.handleIntent(KLogViewerIntent.CopySelected)

        expectThat(viewModel.state.value.activeTab?.activeWindow?.selectedIndices.orEmpty())
            .isEqualTo(setOf(2, 0))
        verify(exactly = 1) { clipboard.copy("line-1\nline-3") }
    }

    private fun seedWindowState(logs: List<LogEntry>, selectedIndices: Set<Int>) {
        val stateFlowField = KLogViewerViewModel::class.java.getDeclaredField("_state").apply {
            isAccessible = true
        }
        @Suppress("UNCHECKED_CAST")
        val stateFlow = stateFlowField.get(viewModel) as MutableStateFlow<KLogViewerState>
        stateFlow.value = stateFlow.value.updateActiveWindow { window ->
            window.copy(
                logs = logs,
                filteredLogs = logs,
                selectedIndices = selectedIndices,
                lastSelectedIndex = selectedIndices.maxOrNull()
            )
        }
    }

    private fun logEntry(content: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("2026-06-02T09:33:00Z"),
        level = LogLevel.INFO,
        content = LogContent(content)
    )

}
