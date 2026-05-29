package com.klogviewer.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.logList
import com.klogviewer.ui.robot.mainRobot
import com.klogviewer.ui.robot.sidebar
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import java.io.File

@OptIn(ExperimentalTestApi::class)
class KLogViewerUiTest {

    private val logSource = mockk<LogSource>()
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>()

    private val testLogFile = File.createTempFile("test", ".log").apply { deleteOnExit() }
    private val testLogPath = testLogFile.absolutePath
    private val testEntries = listOf(
        LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("First log message")),
        LogEntry(LogTimestamp("2023-01-01 10:00:01"), LogLevel.ERROR, LogContent("Second log message (error)")),
        LogEntry(LogTimestamp("2023-01-01 10:00:02"), LogLevel.DEBUG, LogContent("Third log message (debug)"))
    )

    private fun ComposeUiTest.setupApp() {
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
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit
        
        // Mock heuristic probe to return a simple parser
        val mockParser = mockk<com.klogviewer.domain.parser.LogParser>()
        every { heuristicProbe.detect(any()) } returns com.klogviewer.core.parser.ProbeResult(
            parser = mockParser,
            columns = listOf("Timestamp", "Level", "Message"),
            parserName = "Simple"
        )

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }
    }

    @Test
    fun givenFileSelected_whenLoaded_thenLogsAreDisplayed() = runComposeUiTest {
        setupApp()
        
        // Mock file selection
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        
        // Mock log source to return test entries
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        mainRobot {
            clickAddFile()
        }

        logList {
            assertLogCount(3)
            assertHasText("First log message")
            assertHasText("Second log message (error)")
            assertHasText("Third log message (debug)")
        }
    }

    @Test
    fun givenLogsWithoutRawLevelField_whenLoaded_thenInferredLevelsAreNotShownInLogRows() = runComposeUiTest {
        setupApp()

        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        mainRobot {
            clickAddFile()
        }

        logList {
            assertTextDoesNotExist("[INFO]")
            assertTextDoesNotExist("[ERROR]")
            assertTextDoesNotExist("[DEBUG]")
        }
    }

    @Test
    fun givenLogsWithRawLevelField_whenLoaded_thenRawLevelValueIsShownInLogRows() = runComposeUiTest {
        setupApp()

        val entryWithRawLevel = LogEntry(
            timestamp = LogTimestamp("2023-01-01 10:00:03"),
            level = LogLevel.WARN,
            content = LogContent("Fourth log message"),
            fields = mapOf("level" to "APP_WARN")
        )

        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(listOf(entryWithRawLevel)).right()
        )

        mainRobot {
            clickAddFile()
        }

        logList {
            assertTextExists("APP_WARN")
        }
    }

    @Test
    fun givenLogsLoaded_whenLevelFiltered_thenListIsUpdated() = runComposeUiTest {
        setupApp()
        
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        mainRobot {
            clickAddFile()
        }

        // Verify initial state
        logList {
            assertLogCount(3)
        }

        // Toggle DEBUG off
        sidebar {
            toggleLevel(LogLevel.DEBUG)
        }

        // Verify DEBUG log is gone
        logList {
            assertLogCount(2)
            assertTextExists("First log message")
            assertTextExists("Second log message (error)")
            assertTextDoesNotExist("Third log message (debug)")
        }
    }

    @Test
    fun givenLogsLoaded_whenSearchTermEntered_thenLogsAreFiltered() = runComposeUiTest {
        setupApp()
        
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(testEntries).right()
        )

        mainRobot {
            clickAddFile()
        }

        // Search for "error"
        mainRobot {
            typeFilter("error")
        }

        // Verify only error log remains
        logList {
            assertLogCount(1)
            assertTextExists("Second log message (error)")
            assertTextDoesNotExist("First log message")
        }
        
        // Clear filter
        mainRobot {
            clearFilter()
        }
        
        // Verify all logs are back
        logList {
            assertLogCount(3)
        }
    }
}
