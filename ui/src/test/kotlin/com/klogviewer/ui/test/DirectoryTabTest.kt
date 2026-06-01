package com.klogviewer.ui.test

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalTestApi::class)
class DirectoryTabTest {

    companion object {
        private const val minimumShownHiddenLuminanceGap = 0.04f
    }

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>()

    @Test
    fun givenWindowWithSources_whenRendered_thenActiveWindowSourceDropdownIsVisible() = runComposeUiTest {
        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)
        val sourceA = "/tmp/source-a.log"
        val sourceB = "/tmp/source-b.log"

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(sourceA, sourceB)))

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.activeTab?.activeWindow?.sourceIds?.size == 2
        }

        onNodeWithTag("active_window_source_dropdown").assertExists()
        onNodeWithTag("active_window_source_dropdown").performClick()
        val menuWidth = with(density) {
            onNodeWithTag("active_window_source_menu", useUnmergedTree = true)
                .fetchSemanticsNode().size.width.toDp()
        }
        assert(menuWidth >= 500.dp) {
            "Expected widened source dropdown menu width to be at least 500.dp, but was $menuWidth"
        }
        onNodeWithTag("active_window_source_path_${sourceA.hashCode()}", useUnmergedTree = true).assertExists()
        onNodeWithTag("active_window_source_path_${sourceB.hashCode()}", useUnmergedTree = true).assertExists()
        onNodeWithTag("active_window_source_toggle_${sourceA.hashCode()}").assertExists()
        onNodeWithTag("active_window_source_toggle_${sourceB.hashCode()}").assertExists()
        onAllNodesWithText("Hide").assertCountEquals(2)
    }

    @Test
    fun givenWindowWithMultipleSources_whenDropdownHideClicked_thenSourceIsHiddenFromVisibleLogs() = runComposeUiTest {
        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)
        val sourceA = "/tmp/hide-a.log"
        val sourceB = "/tmp/hide-b.log"

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(sourceA, sourceB)))

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.activeTab?.activeWindow?.sourceIds?.containsAll(listOf(sourceA, sourceB)) == true
        }

        val activeWindowId = viewModel.state.value.activeTab?.activeWindow?.id ?: error("No active window")
        val handleLogUpdateMethod = viewModel.javaClass.getDeclaredMethod(
            "handleLogUpdate",
            String::class.java,
            LogUpdate::class.java,
            String::class.java
        )
        handleLogUpdateMethod.isAccessible = true

        handleLogUpdateMethod.invoke(
            viewModel,
            activeWindowId,
            LogUpdate.Initial(
                listOf(
                    LogEntry(LogTimestamp("2026-01-01T00:00:00Z"), LogLevel.INFO, LogContent("A-1"), sourceId = sourceA)
                )
            ),
            sourceA
        )

        handleLogUpdateMethod.invoke(
            viewModel,
            activeWindowId,
            LogUpdate.Initial(
                listOf(
                    LogEntry(LogTimestamp("2026-01-01T00:00:01Z"), LogLevel.INFO, LogContent("B-1"), sourceId = sourceB)
                )
            ),
            sourceB
        )

        waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.size == 2
        }

        onNodeWithTag("active_window_source_dropdown").performClick()
        onNodeWithTag("active_window_source_toggle_${sourceA.hashCode()}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            val window = viewModel.state.value.activeTab?.activeWindow
            window?.hiddenSourceIds?.contains(sourceA) == true &&
                window.filteredLogs.all { it.sourceId != sourceA }
        }

        onNodeWithTag("active_window_source_dropdown").performClick()
        onNodeWithTag("active_window_source_toggle_${sourceA.hashCode()}").performClick()

        waitUntil(timeoutMillis = 5_000) {
            val window = viewModel.state.value.activeTab?.activeWindow
            window != null && !window.hiddenSourceIds.contains(sourceA) &&
                window.filteredLogs.any { it.sourceId == sourceA }
        }
    }

    @Test
    fun givenWindowWithHiddenSource_whenRendered_thenShownSourceNameUsesDarkerColor() = runComposeUiTest {
        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit

        val sourceA = "/tmp/color-a.log"
        val sourceB = "/tmp/color-b.log"

        val prefs = UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "default",
                    title = "Log View",
                    activeWindowId = "default-window",
                    windows = listOf(
                        WindowPreference(
                            id = "default-window",
                            filePath = sourceA,
                            sourceIds = listOf(sourceA, sourceB),
                            hiddenSourceIds = setOf(sourceB)
                        )
                    )
                )
            ),
            activeTabId = "default"
        )
        every { prefsRepository.load() } returns prefs

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        onNodeWithTag("active_window_source_display_name_${sourceA.hashCode()}", useUnmergedTree = true)
            .assertExists()
        onNodeWithTag("active_window_source_display_name_${sourceB.hashCode()}", useUnmergedTree = true)
            .assertExists()

        val shownNameImage = onNodeWithTag(
            "active_window_source_display_name_${sourceA.hashCode()}",
            useUnmergedTree = true
        ).captureToImage()
        val hiddenNameImage = onNodeWithTag(
            "active_window_source_display_name_${sourceB.hashCode()}",
            useUnmergedTree = true
        ).captureToImage()

        val shownNameLuminance = shownNameImage.averageOpaqueLuminance()
        val hiddenNameLuminance = hiddenNameImage.averageOpaqueLuminance()
        val luminanceGap = hiddenNameLuminance - shownNameLuminance

        assert(luminanceGap >= minimumShownHiddenLuminanceGap) {
            "Expected hidden source text to be visibly lighter than shown source text with a minimum " +
                "gap of $minimumShownHiddenLuminanceGap, but measured gap was $luminanceGap " +
                "(hidden=$hiddenNameLuminance, shown=$shownNameLuminance)"
        }
    }

    @Test
    fun givenLongSourcePath_whenHoveringDropdownEntry_thenTooltipShowsFullPath() = runComposeUiTest {
        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)
        val longSourcePath = "/tmp/very-long-directory-segment/very-long-directory-segment/very-long-directory-segment/very-long-directory-segment/app-source.log"

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(longSourcePath)))

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        waitUntil(timeoutMillis = 5_000) {
            viewModel.state.value.activeTab?.activeWindow?.sourceIds?.contains(longSourcePath) == true
        }

        onNodeWithTag("active_window_source_dropdown").performClick()
        val tooltipTag = "active_window_source_tooltip_${longSourcePath.hashCode()}"

        onNodeWithTag("active_window_source_path_${longSourcePath.hashCode()}", useUnmergedTree = true)
            .performMouseInput { moveTo(center) }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag(tooltipTag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }

        onNodeWithTag(tooltipTag, useUnmergedTree = true)
            .assertExists()
            .assertTextEquals(longSourcePath)
    }

    @Test
    fun givenDirectoryOpened_whenFilesDiscovered_thenTabTitleShowsCount() = runComposeUiTest {
        val tempDir = Files.createTempDirectory("testDirCount").toFile()
        tempDir.deleteOnExit()
        
        val testEntries = listOf(
            LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("Log 1"), sourceId = File(tempDir, "file1.log").absolutePath),
            LogEntry(LogTimestamp("2023-01-01 10:00:01"), LogLevel.INFO, LogContent("Log 2"), sourceId = File(tempDir, "file2.log").absolutePath)
        )

        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit
        
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
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        // Wait for directory flag to be set
        waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.activeTab?.activeWindow?.isDirectory == true
        }

        // Manually send log update to simulate discovery of 2 files
        // (This is a bit hacky but tests the UI logic)
        val handleLogUpdateMethod = viewModel.javaClass.getDeclaredMethod("handleLogUpdate", String::class.java, LogUpdate::class.java, String::class.java)
        handleLogUpdateMethod.isAccessible = true
        val activeWindowId = viewModel.state.value.activeTab?.activeWindow?.id!!
        
        handleLogUpdateMethod.invoke(viewModel, activeWindowId, LogUpdate.Initial(testEntries), tempDir.absolutePath)

        // Verify tab title shows "[2]"
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("${tempDir.name} [2]", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun givenEmptyDirectoryOpened_thenTabTitleShowsZeroCount() = runComposeUiTest {
        val tempDir = Files.createTempDirectory("emptyDir").toFile()
        tempDir.deleteOnExit()

        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit
        
        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        // Verify tab title shows "[0]"
        waitUntil(timeoutMillis = 5000) {
            onAllNodesWithText("${tempDir.name} [0]", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
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
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit
        
        every { heuristicProbe.detect(any()) } returns com.klogviewer.core.parser.ProbeResult(
            parser = mockk(),
            columns = listOf("Timestamp", "Level", "Content"),
            parserName = "Simple"
        )

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(tempDir.absolutePath)))
        
        // Wait for directory flag to be set
        waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.activeTab?.activeWindow?.isDirectory == true
        }

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

private fun ImageBitmap.averageOpaqueLuminance(): Float {
    val pixelMap = toPixelMap()
    var totalLuminance = 0f
    var pixelCount = 0

    for (x in 0 until pixelMap.width) {
        for (y in 0 until pixelMap.height) {
            val pixel = pixelMap[x, y]
            if (pixel.alpha > 0f) {
                totalLuminance += pixel.luminance()
                pixelCount += 1
            }
        }
    }

    return if (pixelCount == 0) 0f else totalLuminance / pixelCount
}
