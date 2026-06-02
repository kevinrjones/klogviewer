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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FontSelectionPersistenceTest {
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
    fun `given font action when applied then active window font updates`() {
        viewModel.handleIntent(KLogViewerIntent.ApplyLogFont(family = "Monospaced", sizeSp = 15))

        val activeWindow = viewModel.state.value.activeTab?.activeWindow
        expectThat(activeWindow?.logFontFamily).isEqualTo("Monospaced")
        expectThat(activeWindow?.logFontSizeSp).isEqualTo(15)
    }

    @Test
    fun `given font action when applied then font settings are persisted`() {
        viewModel.handleIntent(KLogViewerIntent.ApplyLogFont(family = "Monospaced", sizeSp = 18))

        val savedPrefs = prefsRepo.load()
        val windowPref = savedPrefs.tabs.first().windows.first()
        expectThat(windowPref.logFontFamily).isEqualTo("Monospaced")
        expectThat(windowPref.logFontSizeSp).isEqualTo(18)
    }
}
