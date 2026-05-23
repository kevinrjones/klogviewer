package com.klogviewer.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.TabPreference
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.model.WindowPreference
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.logList
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

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

    @Test
    fun givenApp_whenLineNumberColumnExists_thenItShouldBeResizable() = runComposeUiTest {
        setupApp()
        
        logList {
            // Verify Line # column exists and has a resize handle
            onNode(hasTestTag("resize_handle_Line #"), useUnmergedTree = true).assertExists()
            
            assertColumnWidth("Line #", 50.dp)
            
            resizeColumn("Line #", 20f)
            
            waitForIdle()
            
            // Check that width changed
            val node = onNode(hasTestTag("column_header_Line #"), useUnmergedTree = true).fetchSemanticsNode()
            val width = with(density) { node.size.width.toDp() }
            assert(width != 50.dp) { "Line # column width should have changed, but is still $width" }
        }
    }
}
