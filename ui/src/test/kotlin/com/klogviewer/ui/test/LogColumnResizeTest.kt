package com.klogviewer.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.TabPreference
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.model.WindowPreference
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.components.LogList
import com.klogviewer.ui.robot.logList
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class LogColumnResizeTest {

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>(relaxed = true)

    private fun ComposeUiTest.setupApp() {
        val prefs = UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "Test Tab",
                    windows = listOf(WindowPreference(id = "window1", filePath = "test.log")),
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

    @Test
    fun givenApp_whenLineNumberColumnExists_thenItShouldBeResizable() = runComposeUiTest {
        setupApp()
        
        // Verify log list exists
        onNode(hasTestTag("log_list_window1"), useUnmergedTree = true).assertExists()

        logList(windowId = "window1") {
            // Verify Line # column exists
            onNode(hasTestTag("column_header_gutter"), useUnmergedTree = true).assertExists()

            // Verify Line # column has a resize handle
            onNode(hasTestTag("resize_handle_gutter"), useUnmergedTree = true).assertExists()
            
            assertColumnWidth("gutter", 50.dp)
            
            resizeColumn("gutter", 20f)
            
            waitForIdle()
            
            // Check that width changed
            val node = onNode(hasTestTag("column_header_gutter"), useUnmergedTree = true).fetchSemanticsNode()
            val width = with(density) { node.size.width.toDp() }
            assert(width != 50.dp) { "Line # column width should have changed, but is still $width" }
        }
    }

    @Test
    fun givenApp_whenLastColumnUsesDefaultWidth_thenItIsCappedAndResizable() = runComposeUiTest {
        setupApp()

        onNode(hasTestTag("log_list_window1"), useUnmergedTree = true).assertExists()

        logList(windowId = "window1") {
            onNode(hasTestTag("column_header_Message"), useUnmergedTree = true).assertExists()
            onNode(hasTestTag("resize_handle_Message"), useUnmergedTree = true).assertExists()

            assertColumnWidth("Message", 300.dp)

            resizeColumn("Message", 40f)
            waitForIdle()

            val node = onNode(hasTestTag("column_header_Message"), useUnmergedTree = true).fetchSemanticsNode()
            val width = with(density) { node.size.width.toDp() }
            assert(width > 300.dp) { "Message column width should have increased after resize, but is $width" }
        }
    }

    @Test
    fun givenApp_whenLastColumnIsResizedRightTwice_thenSecondResizeAlsoIncreasesWidth() = runComposeUiTest {
        setupApp()

        onNode(hasTestTag("log_list_window1"), useUnmergedTree = true).assertExists()

        logList(windowId = "window1") {
            onNode(hasTestTag("column_header_Message"), useUnmergedTree = true).assertExists()
            onNode(hasTestTag("resize_handle_Message"), useUnmergedTree = true).assertExists()

            resizeColumn("Message", 40f)
            waitForIdle()

            val firstWidth = with(density) {
                onNode(hasTestTag("column_header_Message"), useUnmergedTree = true)
                    .fetchSemanticsNode().size.width.toDp()
            }

            resizeColumn("Message", 40f)
            waitForIdle()

            val secondWidth = with(density) {
                onNode(hasTestTag("column_header_Message"), useUnmergedTree = true)
                    .fetchSemanticsNode().size.width.toDp()
            }

            assert(secondWidth > firstWidth) {
                "Message column width should continue increasing on repeated right resize, but first=$firstWidth second=$secondWidth"
            }
        }
    }

    @Test
    fun givenApp_whenLastColumnDraggedFarRight_thenWidthIncreasesSubstantially() = runComposeUiTest {
        setupApp()

        onNode(hasTestTag("log_list_window1"), useUnmergedTree = true).assertExists()

        logList(windowId = "window1") {
            onNode(hasTestTag("column_header_Message"), useUnmergedTree = true).assertExists()
            onNode(hasTestTag("resize_handle_Message"), useUnmergedTree = true).assertExists()

            resizeColumn("Message", 300f)
            waitForIdle()

            val width = with(density) {
                onNode(hasTestTag("column_header_Message"), useUnmergedTree = true)
                    .fetchSemanticsNode().size.width.toDp()
            }

            assert(width > 500.dp) {
                "Message column width should increase substantially on large right drag, but is $width"
            }
        }
    }

    @Test
    fun givenApp_whenMessageColumnHeaderRendered_thenResizeHandleIsAtColumnEdge() = runComposeUiTest {
        setupApp()

        onNode(hasTestTag("log_list_window1"), useUnmergedTree = true).assertExists()

        val header = onNode(hasTestTag("column_header_Message"), useUnmergedTree = true)
            .fetchSemanticsNode()
        val handle = onNode(hasTestTag("resize_handle_Message"), useUnmergedTree = true)
            .fetchSemanticsNode()

        val rightEdgeDelta = abs(header.boundsInRoot.right - handle.boundsInRoot.right)

        assert(rightEdgeDelta <= 1f) {
            "Message resize handle should align with column right edge, but delta is $rightEdgeDelta px"
        }
    }

    @Test
    fun givenNarrowMessageColumn_whenMessageIsLong_thenRowHeightIncreasesWithWrappedText() = runComposeUiTest {
        val shortEntry = LogEntry(
            timestamp = LogTimestamp("2026-05-25T12:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("short message")
        )
        val longEntry = LogEntry(
            timestamp = LogTimestamp("2026-05-25T12:00:01Z"),
            level = LogLevel.INFO,
            content = LogContent("This is a long message that should wrap across multiple lines when the message column is narrow. ".repeat(5))
        )

        setContent {
            LogList(
                logs = listOf(shortEntry, longEntry),
                filterQueries = emptyList(),
                isDarkMode = false,
                columns = listOf("Message"),
                columnWidths = mapOf("Message" to 80),
                isAutoScrollEnabled = false,
                windowId = "wrap-test"
            )
        }

        waitForIdle()

        val rowNodes = onAllNodes(hasTestTag("log_entry_row"), useUnmergedTree = true).fetchSemanticsNodes()
        assert(rowNodes.size >= 2) { "Expected at least two log rows, but found ${rowNodes.size}" }

        val firstRowHeight = rowNodes[0].size.height
        val secondRowHeight = rowNodes[1].size.height

        assert(secondRowHeight > firstRowHeight) {
            "Expected long message row to be taller due to wrapping, but first=$firstRowHeight second=$secondRowHeight"
        }
    }

    @Test
    fun givenMessageColumnShrinksAfterBeingWide_whenListRecomposes_thenContentWidthAlsoShrinks() = runComposeUiTest {
        val entry = LogEntry(
            timestamp = LogTimestamp("2026-05-25T12:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("message")
        )
        var messageWidth by mutableStateOf(1200)

        setContent {
            Box(modifier = Modifier.width(900.dp).height(300.dp)) {
                LogList(
                    logs = listOf(entry),
                    filterQueries = emptyList(),
                    isDarkMode = false,
                    columns = listOf("Timestamp", "Level", "Message"),
                    columnWidths = mapOf("Message" to messageWidth),
                    isAutoScrollEnabled = false,
                    windowId = "shrink-test",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        waitForIdle()

        val expandedWidthPx = onNode(hasTestTag("log_lazy_column"), useUnmergedTree = true)
            .fetchSemanticsNode().size.width

        runOnIdle {
            messageWidth = 300
        }
        waitForIdle()

        val shrunkWidthPx = onNode(hasTestTag("log_lazy_column"), useUnmergedTree = true)
            .fetchSemanticsNode().size.width

        assert(shrunkWidthPx < expandedWidthPx) {
            "Log list content width should shrink after reducing message column width, but expanded=$expandedWidthPx shrunk=$shrunkWidthPx"
        }
    }
}
