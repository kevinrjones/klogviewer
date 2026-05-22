package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PersistenceIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    private var viewModel: KLogViewerViewModel? = null

    @AfterEach
    fun tearDown() = runBlocking {
        viewModel?.clear()
        kotlinx.coroutines.delay(200.milliseconds)
    }

    @Test
    fun `should restore tabs and windows and reload logs on startup`(): Unit = runBlocking {
        val logFile = File(tempDir, "test.log")
        logFile.writeText("2023-10-27 10:00:00 INFO Test message\n")

        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        
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
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // Wait for logs to load
        withTimeout(5.seconds) {
            vm.state.first { state ->
                val window = state.tabs.find { it.id == "tab-1" }?.windows?.find { it.id == "win-1" }
                window != null && !window.isLoading && window.logs.isNotEmpty()
            }
        }

        val state = vm.state.value
        expectThat(state.tabs).hasSize(1)
        val tab = state.tabs[0]
        expectThat(tab.id).isEqualTo("tab-1")
        expectThat(tab.title).isEqualTo("test.log") // Refreshed from file name
        expectThat(tab.windows).hasSize(1)
        val window = tab.windows[0]
        expectThat(window.id).isEqualTo("win-1")
        expectThat(window.logs).hasSize(1)
        expectThat(window.logs[0].content.value).isEqualTo("Test message")
    }

    @Test
    fun `should save state changes to preferences`(): Unit = runBlocking {
        val logFile = File(tempDir, "test.log")
        logFile.writeText("2023-10-27 10:00:00 INFO Test message\n")

        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // Add a tab and load a file
        vm.handleIntent(KLogViewerIntent.AddTab)
        val activeTabId = vm.state.value.activeTabId!!
        
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(logFile.absolutePath)))

        // Wait for loading to start (so savePreferences is called)
        withTimeout(2.seconds) {
            vm.state.first { it.activeTab?.activeWindow?.isLoading == true || it.activeTab?.activeWindow?.logs?.isNotEmpty() == true }
        }

        // Verify preferences were saved
        withTimeout(2.seconds) {
            while (prefsRepo.load().tabs.find { it.id == activeTabId }?.windows?.firstOrNull()?.sourceIds?.isNotEmpty() != true) {
                kotlinx.coroutines.delay(100.milliseconds)
            }
        }
        val savedPrefs = prefsRepo.load()
        expectThat(savedPrefs.tabs).hasSize(2) // Default + New Tab
        val newTabPref = savedPrefs.tabs.find { it.id == activeTabId }
        expectThat(newTabPref).isNotNull()
        expectThat(newTabPref?.windows?.get(0)?.sourceIds).isEqualTo(listOf(logFile.absolutePath))
    }

    @Test
    fun `should persist column widths`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // Resize a column
        val activeWindowId = vm.state.value.activeTab?.activeWindowId!!
        vm.handleIntent(KLogViewerIntent.UpdateColumnWidth(activeWindowId, "Timestamp", 250))

        // Verify state
        val window = vm.state.value.activeTab?.activeWindow
        expectThat(window?.columnWidths?.get("Timestamp")).isEqualTo(250)

        // Verify preferences were saved (with debounce)
        val activeTabId = vm.state.value.activeTabId
        
        val savedPrefs = withTimeout(2.seconds) {
            var prefs = prefsRepo.load()
            while (prefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }?.columnWidths?.get("Timestamp") != 250) {
                kotlinx.coroutines.delay(100.milliseconds)
                prefs = prefsRepo.load()
            }
            prefs
        }
        val savedWindow = savedPrefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }
        
        expectThat(savedWindow?.columnWidths?.get("Timestamp")).isEqualTo(250)
    }

    @Test
    fun `should persist auto-scroll state`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // Toggle auto-scroll (default is true, so toggle to false)
        vm.handleIntent(KLogViewerIntent.ToggleAutoScroll)

        // Verify state
        val window = vm.state.value.activeTab?.activeWindow
        expectThat(window?.isAutoScrollEnabled).isEqualTo(false)

        // Verify preferences were saved
        val activeTabId = vm.state.value.activeTabId
        val activeWindowId = vm.state.value.activeTab?.activeWindowId
        
        val savedPrefs = withTimeout(2.seconds) {
            var prefs = prefsRepo.load()
            while (prefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }?.isAutoScrollEnabled != false) {
                kotlinx.coroutines.delay(100.milliseconds)
                prefs = prefsRepo.load()
            }
            prefs
        }
        val savedWindow = savedPrefs.tabs.find { it.id == activeTabId }?.windows?.find { it.id == activeWindowId }
        
        expectThat(savedWindow?.isAutoScrollEnabled).isEqualTo(false)
    }
}
