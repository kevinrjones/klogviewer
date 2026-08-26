package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import io.mockk.mockk
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
import strikt.assertions.isTrue
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CompactCellModePersistenceTest {
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
    fun `given default state when app loads then useCompactCellMode is true`() {
        val activeWindow = viewModel.state.value.activeTab?.activeWindow
        expectThat(activeWindow?.useCompactCellMode).isTrue()
    }

    @Test
    fun `given toggle intent when applied then active window useCompactCellMode toggles to false`() {
        viewModel.handleIntent(KLogViewerIntent.ToggleCompactCellMode)

        val activeWindow = viewModel.state.value.activeTab?.activeWindow
        expectThat(activeWindow?.useCompactCellMode).isFalse()
    }

    @Test
    fun `given toggle intent when applied twice then useCompactCellMode returns to true`() {
        viewModel.handleIntent(KLogViewerIntent.ToggleCompactCellMode)
        viewModel.handleIntent(KLogViewerIntent.ToggleCompactCellMode)

        val activeWindow = viewModel.state.value.activeTab?.activeWindow
        expectThat(activeWindow?.useCompactCellMode).isTrue()
    }

    @Test
    fun `given toggle intent when applied then useCompactCellMode is persisted`() {
        viewModel.handleIntent(KLogViewerIntent.ToggleCompactCellMode)

        val savedPrefs = prefsRepo.load()
        val windowPref = savedPrefs.tabs.first().windows.first()
        expectThat(windowPref.useCompactCellMode).isFalse()
    }

    @Test
    fun `given toggled state when app reloads then useCompactCellMode persists across sessions`() {
        viewModel.handleIntent(KLogViewerIntent.ToggleCompactCellMode)

        // Simulate app reload by creating a new ViewModel with the same prefs repo
        viewModel.clear()
        val newViewModel = KLogViewerViewModel(
            logSource = mockLogSource,
            prefsRepository = prefsRepo,
            heuristicProbe = HeuristicProbe(ParserRegistry())
        )

        val activeWindow = newViewModel.state.value.activeTab?.activeWindow
        expectThat(activeWindow?.useCompactCellMode).isFalse()
        newViewModel.clear()
    }
}
