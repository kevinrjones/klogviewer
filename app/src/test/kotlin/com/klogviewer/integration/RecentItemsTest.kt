package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.*
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class RecentItemsTest {
    @TempDir
    lateinit var tempDir: File

    private var viewModel: KLogViewerViewModel? = null

    @AfterEach
    fun tearDown() = runBlocking {
        viewModel?.clear()
        kotlinx.coroutines.delay(200.milliseconds) // Give Windows time to release file handles
    }

    @Test
    fun `should filter missing items and offer to delete them`(): Unit = runBlocking {
        val existingFile = File(tempDir, "existing.log")
        existingFile.createNewFile()
        
        val missingFile = File(tempDir, "to-be-deleted.log")
        missingFile.createNewFile()
        
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // 1. Add both to recent items while they exist
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(missingFile.absolutePath)))
        // Wait for the job to start and the file to be opened
        kotlinx.coroutines.delay(100.milliseconds)
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(existingFile.absolutePath))) // Load existing last so missing is not locked
        // Wait for the job to start and the file to be opened
        kotlinx.coroutines.delay(100.milliseconds)

        expectThat(vm.state.value.recentFiles).hasSize(2)
        expectThat(vm.state.value.recentFiles).contains(existingFile.absolutePath)
        expectThat(vm.state.value.recentFiles).contains(missingFile.absolutePath)

        // 2. Delete one file
        missingFile.delete()

        // 3. Clear missing items (this mimics the "Offer to delete")
        vm.handleIntent(KLogViewerIntent.ClearMissingRecentItems)

        expectThat(vm.state.value.recentFiles).hasSize(1)
        expectThat(vm.state.value.recentFiles).contains(existingFile.absolutePath)
        expectThat(vm.state.value.recentFiles.contains(missingFile.absolutePath)).isFalse()
    }

    @Test
    fun `should remove specific recent item`(): Unit = runBlocking {
        val file1 = File(tempDir, "file1.log")
        file1.createNewFile()
        val file2 = File(tempDir, "file2.log")
        file2.createNewFile()
        
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath, file2.absolutePath)))

        expectThat(vm.state.value.recentFiles).hasSize(2)

        vm.handleIntent(KLogViewerIntent.RemoveRecentItem(file1.absolutePath))

        expectThat(vm.state.value.recentFiles).hasSize(1)
        expectThat(vm.state.value.recentFiles).contains(file2.absolutePath)
        expectThat(vm.state.value.recentFiles.contains(file1.absolutePath)).isFalse()
    }

    @Test
    fun `should not show missing file dialog when clicking missing item but mark window as missing`(): Unit = runBlocking {
        val existingFile = File(tempDir, "existing.log")
        existingFile.writeText("some logs")
        
        val missingFile = File(tempDir, "missing.log")
        
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // 1. Load existing file
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(existingFile.absolutePath)))
        // Wait for logs to load and file to be opened
        kotlinx.coroutines.delay(200.milliseconds)
        
        // 2. Try to load missing file
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(missingFile.absolutePath)))
        // In the UI, the RecentItemsDialog would call DismissDialog, but here we call LoadFiles directly.
        // loadFilesIntoWindow no longer sets pendingDialog, so it should stay null or unchanged.
        
        // Wait for potential background processing
        kotlinx.coroutines.delay(200.milliseconds)

        // 3. Verify NO dialog is shown
        expectThat(vm.state.value.pendingDialog).isNull()
        
        // 4. Verify window is marked as missing
        val window = vm.state.value.activeTab?.activeWindow
        expectThat(window).isNotNull().and {
            get { filePath }.isEqualTo(missingFile.absolutePath)
            get { missingSourceIds }.contains(missingFile.absolutePath)
            get { error }.isNotNull()
        }
    }
}
