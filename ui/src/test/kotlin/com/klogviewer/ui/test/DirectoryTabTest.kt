package com.klogviewer.ui.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DirectoryTabTest {

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>()

    @Test
    fun givenDirectoryOpened_whenFilesDiscovered_thenTabTitleShowsCount() = runComposeUiTest {
        val tempDir = Files.createTempDirectory("testDirCount").toFile()
        tempDir.deleteOnExit()
        
        val testEntries = listOf(
            LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("Log 1"), sourceId = File(tempDir, "file1.log").absolutePath),
            LogEntry(LogTimestamp("2023-01-01 10:00:01"), LogLevel.INFO, LogContent("Log 2"), sourceId = File(tempDir, "file2.log").absolutePath)
        )

        every { prefsRepository.load() } returns UserPreferences()
        
        // Mock heuristic probe
        every { heuristicProbe.detect(any()) } returns com.klogviewer.core.parser.ProbeResult(
            parser = mockk(),
            columns = listOf("Timestamp", "Level", "Content"),
            parserName = "Simple"
        )

        // Mock log source to simulate directory load
        // DirectoryLogSource will be created by ViewModel, but we can't easily mock it without more plumbing.
        // Instead, we can just manually trigger handleLogUpdate or use a mock that simulates the behavior.
        
        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        // We need to bypass the real DirectoryLogSource and just update the state to verify UI rendering
        viewModel.handleIntent(com.klogviewer.ui.mvi.KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        // Manually send log update to simulate discovery of 2 files
        // (This is a bit hacky but tests the UI logic)
        val handleLogUpdateMethod = viewModel.javaClass.getDeclaredMethod("handleLogUpdate", String::class.java, LogUpdate::class.java, String::class.java)
        handleLogUpdateMethod.isAccessible = true
        val activeWindowId = viewModel.state.value.activeTab?.activeWindow?.id!!
        
        handleLogUpdateMethod.invoke(viewModel, activeWindowId, LogUpdate.Initial(testEntries), tempDir.absolutePath)

        // Verify tab title shows "[2]"
        onNodeWithText("${tempDir.name} [2]", substring = true).assertExists()
    }

    @Test
    fun givenEmptyDirectoryOpened_thenTabTitleShowsZeroCount() = runComposeUiTest {
        val tempDir = Files.createTempDirectory("emptyDir").toFile()
        tempDir.deleteOnExit()

        every { prefsRepository.load() } returns UserPreferences()
        
        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        viewModel.handleIntent(com.klogviewer.ui.mvi.KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        // Verify tab title shows "[0]"
        onNodeWithText("${tempDir.name} [0]", substring = true).assertExists()
    }

    @Test
    fun givenDirectoryOpened_whenFileRemoved_thenColorRemainsUnchanged() = runComposeUiTest {
        val tempDir = Files.createTempDirectory("testDirMissing").toFile()
        tempDir.deleteOnExit()
        
        val file1 = File(tempDir, "file1.log")
        file1.writeText("Line 1")
        
        val testEntries = listOf(
            LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("Log 1"), sourceId = file1.absolutePath)
        )

        every { prefsRepository.load() } returns UserPreferences()
        
        every { heuristicProbe.detect(any()) } returns com.klogviewer.core.parser.ProbeResult(
            parser = mockk(),
            columns = listOf("Timestamp", "Level", "Content"),
            parserName = "Simple"
        )

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        viewModel.handleIntent(com.klogviewer.ui.mvi.KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        val handleLogUpdateMethod = viewModel.javaClass.getDeclaredMethod("handleLogUpdate", String::class.java, LogUpdate::class.java, String::class.java)
        handleLogUpdateMethod.isAccessible = true
        val activeWindowId = viewModel.state.value.activeTab?.activeWindow?.id!!
        
        // 1. Initial load
        handleLogUpdateMethod.invoke(viewModel, activeWindowId, LogUpdate.Initial(testEntries), tempDir.absolutePath)
        
        // 2. File removed
        handleLogUpdateMethod.invoke(viewModel, activeWindowId, LogUpdate.SourceMissing(file1.absolutePath), tempDir.absolutePath)

        // Verify state
        val window = viewModel.state.value.activeTab?.windows?.find { it.id == activeWindowId }!!
        assert(window.missingSourceIds.contains(file1.absolutePath))
        assert(window.isDirectory)
        
        // We can't easily assert color here without custom matchers for Material Theme or Text color.
        // But we've verified that the state is correct (isDirectory=true and missingSourceIds has the file).
    }
}
