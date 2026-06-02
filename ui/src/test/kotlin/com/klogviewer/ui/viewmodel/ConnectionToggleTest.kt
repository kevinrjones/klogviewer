package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionToggleTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var prefsRepo: JsonPreferencesRepository
    private lateinit var mockLogSource: LogSource
    private lateinit var viewModel: KLogViewerViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        prefsRepo = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        mockLogSource = mockk(relaxed = true)
        
        viewModel = KLogViewerViewModel(
            logSource = mockLogSource,
            prefsRepository = prefsRepo,
            heuristicProbe = HeuristicProbe(ParserRegistry())
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.clear()
    }

    @Test
    fun `should stop log observation when disconnected`() {
        val testFile = File(tempDir, "test.log").apply { writeText("line1\n") }
        
        // Initial load
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        
        expectThat(viewModel.state.value.activeTab?.activeWindow?.isConnected).isEqualTo(true)
        verify(exactly = 1) { mockLogSource.observeLogs(any(), any()) }
        
        // Toggle disconnect
        viewModel.handleIntent(KLogViewerIntent.ToggleConnection)
        
        expectThat(viewModel.state.value.activeTab?.activeWindow?.isConnected).isEqualTo(false)
        // verify that the job was cancelled - difficult to verify directly on mockk but we can check if it reloads on reconnect
    }

    @Test
    fun `should restart log observation when reconnected`() {
        val testFile = File(tempDir, "test.log").apply { writeText("line1\n") }
        
        // Initial load
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        
        // Disconnect
        viewModel.handleIntent(KLogViewerIntent.ToggleConnection)
        clearMocks(mockLogSource)
        
        // Reconnect
        viewModel.handleIntent(KLogViewerIntent.ToggleConnection)
        
        expectThat(viewModel.state.value.activeTab?.activeWindow?.isConnected).isEqualTo(true)
        verify(exactly = 1) { mockLogSource.observeLogs(any(), any()) }
    }

    @Test
    fun `should persist connection state in preferences`() {
        val testFile = File(tempDir, "test.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        
        // Disconnect
        viewModel.handleIntent(KLogViewerIntent.ToggleConnection)
        
        val savedPrefs = prefsRepo.load()
        val windowPref = savedPrefs.tabs.first().windows.first()
        expectThat(windowPref.isConnected).isFalse()
    }

    @Test
    fun `should reconnect and preserve active workspace on refresh when connected`() {
        val testFile = File(tempDir, "test.log").apply { writeText("line1\n") }

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        val initialTabId = viewModel.state.value.activeTabId
        val initialWindowId = viewModel.state.value.activeTab?.activeWindow?.id

        clearMocks(mockLogSource)
        viewModel.handleIntent(KLogViewerIntent.RefreshConnection)

        expectThat(viewModel.state.value.activeTab?.activeWindow?.isConnected).isEqualTo(true)
        expectThat(viewModel.state.value.activeTabId).isEqualTo(initialTabId)
        expectThat(viewModel.state.value.activeTab?.activeWindow?.id).isEqualTo(initialWindowId)
        verify(exactly = 1) { mockLogSource.observeLogs(any(), any()) }
    }

    @Test
    fun `should reconnect on refresh when disconnected`() {
        val testFile = File(tempDir, "test.log").apply { writeText("line1\n") }

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        viewModel.handleIntent(KLogViewerIntent.ToggleConnection)
        clearMocks(mockLogSource)

        viewModel.handleIntent(KLogViewerIntent.RefreshConnection)

        expectThat(viewModel.state.value.activeTab?.activeWindow?.isConnected).isEqualTo(true)
        verify(exactly = 1) { mockLogSource.observeLogs(any(), any()) }
    }
}
