package com.klogviewer.ui.test

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.*
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class KLogViewerComplexUiTest {

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<PreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>(relaxed = true)

    private fun ComposeUiTest.setupApp(initialState: UserPreferences? = null) {
        val prefs = initialState ?: UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "Test Tab",
                    windows = listOf(WindowPreference(id = "window1", filePath = "")),
                    activeWindowId = "window1"
                )
            ),
            activeTabId = "tab1"
        )
        every { prefsRepository.load() } returns prefs
        
        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }
    }

    private fun ComposeUiTest.assertWidthIsNotEqualTo(interaction: SemanticsNodeInteraction, expectedWidth: Dp) {
        val node = interaction.fetchSemanticsNode()
        val width = with(density) { node.size.width.toDp() }
        if (width == expectedWidth) {
            throw AssertionError("Width $width is equal to $expectedWidth")
        }
    }

    @Test
    fun givenSplitPane_whenColumnResized_thenOnlyTargetWindowIsAffected() = runComposeUiTest {
        setupApp()

        // 1. Split horizontally
        mainRobot {
            splitHorizontal()
        }
        
        waitForIdle()

        // 2. Identify windows
        val windowNodes = onAllNodes(
            SemanticsMatcher("Has window tag") { node ->
                node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("window_") == true
            }
        ).fetchSemanticsNodes()
        
        val windowIds = windowNodes.map { node -> 
            node.config[SemanticsProperties.TestTag].removePrefix("window_") 
        }
        
        val window1Id = windowIds.find { it == "window1" }!!
        val window2Id = windowIds.find { it != "window1" }!!

        // 3. Verify initial width (Level = 80dp)
        window(window1Id) {
            logList { assertColumnWidth("Level", 80.dp) }
        }
        window(window2Id) {
            logList { assertColumnWidth("Level", 80.dp) }
        }

        // 4. Resize window 1
        window(window1Id) {
            logList {
                resizeColumn("Level", 50f)
            }
        }
        
        waitForIdle()

        // 5. Verify window 1 has changed
        val interaction = onNode(hasTestTag("column_header_Level") and hasAnyAncestor(hasTestTag("window_$window1Id")), useUnmergedTree = true)
        assertWidthIsNotEqualTo(interaction, 80.dp)
            
        // 6. Verify window 2 is STILL 80.dp
        window(window2Id) {
            logList { assertColumnWidth("Level", 80.dp) }
        }
    }

    private val testLogFile = File.createTempFile("test", ".log").apply { deleteOnExit() }

    @Test
    fun givenLogsLoaded_whenMultiSelected_thenCorrectRowsAreSelected() = runComposeUiTest {
        val testEntries = (1..5).map { 
            LogEntry(LogTimestamp("10:00:0$it"), LogLevel.INFO, LogContent("Message $it")) 
        }
        every { logSource.observeLogs(any(), any()) } returns flowOf(LogUpdate.Initial(testEntries).right())
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogFile
        
        setupApp()

        mainRobot {
            clickAddFile()
        }

        logList {
            // Click row 0
            clickOnRow(0)
            assertRowSelected(0, true)
            
            // Shift-Click row 2 -> should select 0, 1, 2
            clickOnRowWithModifiers(2, shift = true)
            assertRowSelected(0, true)
            assertRowSelected(1, true)
            assertRowSelected(2, true)
            assertRowSelected(3, false)
            
            // Meta-Click row 4 -> should toggle 4
            clickOnRowWithModifiers(4, meta = true)
            assertRowSelected(0, true)
            assertRowSelected(1, true)
            assertRowSelected(2, true)
            assertRowSelected(3, false)
            assertRowSelected(4, true)
            
            // Meta-Click row 1 -> should toggle 1 off
            clickOnRowWithModifiers(1, meta = true)
            assertRowSelected(0, true)
            assertRowSelected(1, false)
            assertRowSelected(2, true)
            assertRowSelected(4, true)
        }
    }

    @Test
    fun givenMultipleTabs_whenSwitching_thenStateIsMaintained() = runComposeUiTest {
        setupApp()
        
        // Add tab
        mainRobot {
            clickAddTab()
        }
        
        waitForIdle()
        
        // We should have 2 tabs now. Test Tab and New Tab.
        onNodeWithText("New Tab").assertExists().assertIsSelected()
        
        // Switch back to Test Tab
        onNodeWithText("Test Tab").performClick()
        onNodeWithText("Test Tab").assertIsSelected()
    }
}
