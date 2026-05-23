package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File

class FileDeletionTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should detect file deletion and update state with missing source`(): Unit = runBlocking {
        val logFile = File(tempDir, "test.log")
        logFile.writeText("2023-01-01 12:00:00 INFO test message\n")
        
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        
        // Use a test scope to control execution
        val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val viewModel = KLogViewerViewModel(
            logSource = source, 
            prefsRepository = prefsRepo, 
            heuristicProbe = heuristicProbe, 
            scope = testScope
        )

        // 1. Load the file
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(logFile.absolutePath)))
        
        // Wait for initial load
        delay(500)
        expectThat(viewModel.state.value.activeTab?.activeWindow?.logs).isNotNull().hasSize(1)
        expectThat(viewModel.state.value.activeTab?.activeWindow?.missingSourceIds).isNotNull().isEmpty()

        // 2. Delete the file
        logFile.delete()
        
        // 3. Wait for polling (FileLogSource polls every 1s)
        delay(1500)
        
        // 4. Verify that data is still there but missingSourceIds is updated
        val window = viewModel.state.value.activeTab?.activeWindow!!
        expectThat(window.logs).hasSize(1)
        expectThat(window.missingSourceIds).contains(logFile.absolutePath)
        
        testScope.cancel()
    }

    @Test
    fun `should detect file deletion in directory and update state with missing source`(): Unit = runBlocking {
        val logDir = File(tempDir, "logs")
        logDir.mkdirs()
        val logFile = File(logDir, "test.log")
        logFile.writeText("2023-01-01 12:00:00 INFO test message\n")
        
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        
        val testScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val viewModel = KLogViewerViewModel(
            logSource = source, 
            prefsRepository = prefsRepo, 
            heuristicProbe = heuristicProbe, 
            scope = testScope
        )

        // 1. Load the directory
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(logDir.absolutePath)))
        
        // Wait for initial load (DirectoryLogSource has a 5s rescan delay, so we need to wait at least that long if it missed the first check)
        // Actually, it should miss the first check because FileLogSource.observeLogs is async.
        delay(7000)
        expectThat(viewModel.state.value.activeTab?.activeWindow?.logs).isNotNull().hasSize(1)

        // 2. Delete the file inside the directory
        logFile.delete()
        
        // 3. Wait for directory rescan (default 5s)
        delay(7000)
        
        // 4. Verify that missingSourceIds is updated
        val window = viewModel.state.value.activeTab?.activeWindow!!
        expectThat(window.missingSourceIds).contains(logFile.absolutePath)
        
        testScope.cancel()
    }
}
