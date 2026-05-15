package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.io.File
import kotlin.time.Duration.Companion.seconds

class PersistenceIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should restore tabs and windows and reload logs on startup`(): Unit = runBlocking {
        val logFile = File(tempDir, "test.log")
        logFile.writeText("2023-10-27 10:00:00 INFO Test message\n")

        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = PreferencesRepository(prefsDir)
        
        // 1. Setup initial state and save it
        val initialPrefs = UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab-1",
                    title = "Test Tab",
                    activeWindowId = "win-1",
                    windows = listOf(
                        WindowPreference(
                            id = "win-1",
                            filePath = logFile.absolutePath,
                            sourceIds = listOf(logFile.absolutePath)
                        )
                    )
                )
            ),
            activeTabId = "tab-1"
        )
        prefsRepo.save(initialPrefs)

        // 2. Create ViewModel and verify it restores state and reloads logs
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        val viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)

        // Wait for logs to load
        withTimeout(5.seconds) {
            viewModel.state.first { state ->
                val window = state.tabs.find { it.id == "tab-1" }?.windows?.find { it.id == "win-1" }
                window != null && !window.isLoading && window.logs.isNotEmpty()
            }
        }

        val state = viewModel.state.value
        expectThat(state.tabs).hasSize(1)
        val tab = state.tabs[0]
        expectThat(tab.id).isEqualTo("tab-1")
        expectThat(tab.title).isEqualTo("test.log") // Refreshed from file name
        expectThat(tab.windows).hasSize(1)
        val window = tab.windows[0]
        expectThat(window.id).isEqualTo("win-1")
        expectThat(window.logs).hasSize(1)
        expectThat(window.logs[0].content.value).isEqualTo("Test message")
        Unit
    }

    @Test
    fun `should save state changes to preferences`(): Unit = runBlocking {
        val logFile = File(tempDir, "test.log")
        logFile.writeText("2023-10-27 10:00:00 INFO Test message\n")

        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = PreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        val viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)

        // Add a tab and load a file
        viewModel.handleIntent(KLogViewerIntent.AddTab)
        val activeTabId = viewModel.state.value.activeTabId!!
        
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(logFile.absolutePath)))

        // Wait for loading to start (so savePreferences is called)
        withTimeout(2.seconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == true || it.activeTab?.activeWindow?.logs?.isNotEmpty() == true }
        }

        // Verify preferences were saved
        val savedPrefs = prefsRepo.load()
        expectThat(savedPrefs.tabs).hasSize(2) // Default + New Tab
        val newTabPref = savedPrefs.tabs.find { it.id == activeTabId }
        expectThat(newTabPref).isNotNull()
        expectThat(newTabPref?.windows?.get(0)?.sourceIds).isEqualTo(listOf(logFile.absolutePath))
        Unit
    }

    @Test
    fun `should persist column widths`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = PreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        val viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)

        // Resize a column
        viewModel.handleIntent(KLogViewerIntent.UpdateColumnWidth("Timestamp", 250))

        // Verify state
        val window = viewModel.state.value.activeTab?.activeWindow
        expectThat(window?.columnWidths?.get("Timestamp")).isEqualTo(250)

        // Verify preferences were saved (with debounce)
        val activeTabId = viewModel.state.value.activeTabId
        val activeWindowId = viewModel.state.value.activeTab?.activeWindowId
        
        val savedPrefs = withTimeout(2.seconds) {
            var prefs = prefsRepo.load()
            while (prefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }?.columnWidths?.get("Timestamp") != 250) {
                kotlinx.coroutines.delay(100)
                prefs = prefsRepo.load()
            }
            prefs
        }
        val savedWindow = savedPrefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }
        
        expectThat(savedWindow?.columnWidths?.get("Timestamp")).isEqualTo(250)
        Unit
    }

    @Test
    fun `should persist auto-scroll state`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = PreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        val viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)

        // Toggle auto-scroll (default is true, so toggle to false)
        viewModel.handleIntent(KLogViewerIntent.ToggleAutoScroll)

        // Verify state
        val window = viewModel.state.value.activeTab?.activeWindow
        expectThat(window?.isAutoScrollEnabled).isEqualTo(false)

        // Verify preferences were saved
        val activeTabId = viewModel.state.value.activeTabId
        val activeWindowId = viewModel.state.value.activeTab?.activeWindowId
        
        val savedPrefs = withTimeout(2.seconds) {
            var prefs = prefsRepo.load()
            while (prefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }?.isAutoScrollEnabled != false) {
                kotlinx.coroutines.delay(100)
                prefs = prefsRepo.load()
            }
            prefs
        }
        val savedWindow = savedPrefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }
        
        expectThat(savedWindow?.isAutoScrollEnabled).isEqualTo(false)
        Unit
    }
}
