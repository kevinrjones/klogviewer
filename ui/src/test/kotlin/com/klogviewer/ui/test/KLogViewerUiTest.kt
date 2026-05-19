package com.klogviewer.ui.test

import androidx.compose.ui.test.junit4.createComposeRule
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.logList
import com.klogviewer.ui.robot.mainRobot
import com.klogviewer.ui.robot.sidebar
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.io.File

class KLogViewerUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val logSource = mockk<LogSource>()
    private val prefsRepository = mockk<PreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>()

    private val testLogFile = File.createTempFile("test", ".log").apply { deleteOnExit() }
    private val testLogPath = testLogFile.absolutePath
    private val testEntries = listOf(
        LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("First log message")),
        LogEntry(LogTimestamp("2023-01-01 10:00:01"), LogLevel.ERROR, LogContent("Second log message (error)")),
        LogEntry(LogTimestamp("2023-01-01 10:00:02"), LogLevel.DEBUG, LogContent("Third log message (debug)"))
    )

    private fun setupApp() {
        every { prefsRepository.load() } returns UserPreferences(
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
        
        // Mock heuristic probe to return a simple parser
        val mockParser = mockk<com.klogviewer.domain.parser.LogParser>()
        every { heuristicProbe.detect(any()) } returns com.klogviewer.core.parser.ProbeResult(
            parser = mockParser,
            columns = listOf("Timestamp", "Level", "Message")
        )

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        composeTestRule.setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }
    }

    @Test
    fun givenFileSelected_whenLoaded_thenLogsAreDisplayed() {
        setupApp()
        
        // Mock file selection
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns File(testLogPath)
        
        // Mock log source to return test entries
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        composeTestRule.mainRobot {
            clickAddFile()
        }

        composeTestRule.logList {
            assertLogCount(3)
            assertHasText("First log message")
            assertHasText("Second log message (error)")
            assertHasText("Third log message (debug)")
        }
    }

    @Test
    fun givenLogsLoaded_whenLevelFiltered_thenListIsUpdated() {
        setupApp()
        
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns File(testLogPath)
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        composeTestRule.mainRobot {
            clickAddFile()
        }

        // Verify initial state
        composeTestRule.logList {
            assertLogCount(3)
        }

        // Toggle DEBUG off
        composeTestRule.sidebar {
            toggleLevel(LogLevel.DEBUG)
        }

        // Verify DEBUG log is gone
        composeTestRule.logList {
            assertLogCount(2)
            assertTextExists("First log message")
            assertTextExists("Second log message (error)")
            assertTextDoesNotExist("Third log message (debug)")
        }
    }

    @Test
    fun givenLogsLoaded_whenSearchTermEntered_thenLogsAreFiltered() {
        setupApp()
        
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns File(testLogPath)
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        composeTestRule.mainRobot {
            clickAddFile()
        }

        // Search for "error"
        composeTestRule.mainRobot {
            typeFilter("error")
        }

        // Verify only error log remains
        composeTestRule.logList {
            assertLogCount(1)
            assertTextExists("Second log message (error)")
            assertTextDoesNotExist("First log message")
        }
        
        // Clear filter
        composeTestRule.mainRobot {
            clearFilter()
        }
        
        // Verify all logs are back
        composeTestRule.logList {
            assertLogCount(3)
        }
    }
}
