package com.klogviewer.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.ui.components.DialogProvider
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.mainRobot
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import java.io.File
import java.time.Instant

@OptIn(ExperimentalTestApi::class)
class DashboardUxHardeningUiTest {

    private val logSource = mockk<LogSource>()
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)
    private val dialogProvider = mockk<DialogProvider>(relaxed = true)

    private val testLogFile = File.createTempFile("dashboard-ux", ".log").apply { deleteOnExit() }
    private val testLogPath = testLogFile.absolutePath

    private val testEntries = listOf(
        logEntry("2026-01-01T00:00:00Z", "first", LogLevel.INFO, mapOf("service" to "auth")),
        logEntry("2026-01-01T00:00:01Z", "second", LogLevel.ERROR, mapOf("service" to "billing")),
        logEntry("2026-01-01T00:00:02Z", "third", LogLevel.ERROR, mapOf("service" to "billing"))
    )
    private val testEntriesWithRawLevel = listOf(
        logEntry("2026-01-01T00:00:00Z", "first", LogLevel.INFO, mapOf("service" to "auth", "level" to "INFO")),
        logEntry("2026-01-01T00:00:01Z", "second", LogLevel.ERROR, mapOf("service" to "billing", "level" to "ERROR")),
        logEntry("2026-01-01T00:00:02Z", "third", LogLevel.ERROR, mapOf("service" to "billing", "level" to "ERROR"))
    )
    private val testEntriesWithUnknownRawLevel = listOf(
        logEntry("2026-01-01T00:00:00Z", "first", LogLevel.UNKNOWN, mapOf("service" to "auth", "level" to "UNKNOWN")),
        logEntry("2026-01-01T00:00:01Z", "second", LogLevel.UNKNOWN, mapOf("service" to "billing", "level" to "UNKNOWN")),
        logEntry("2026-01-01T00:00:02Z", "third", LogLevel.UNKNOWN, mapOf("service" to "billing", "level" to "UNKNOWN"))
    )

    private fun logEntry(
        instantIso: String,
        message: String,
        level: LogLevel,
        fields: Map<String, String>
    ): LogEntry {
        val instant = Instant.parse(instantIso)
        return LogEntry(
            timestamp = LogTimestamp(instant.toString()),
            level = level,
            content = LogContent(message),
            fields = fields,
            instant = instant
        )
    }

    private fun ComposeUiTest.setupApp(entries: List<LogEntry> = testEntries) {
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
        every { dialogProvider.showOpenFileDialog(any(), any()) } returns testLogPath
        every { dialogProvider.showMessageDialog(any(), any()) } returns Unit
        every { logSource.observeLogs(LogFilePath(testLogPath), any()) } returns flowOf(
            LogUpdate.Initial(entries).right()
        )

        val parser = mockk<LogParser>(relaxed = true)
        every { heuristicProbe.detect(any()) } returns ProbeResult(
            parser = parser,
            columns = listOf("Timestamp", "Level", "Message"),
            parserName = "Simple"
        )

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)
        setContent {
            KLogViewerScreen(viewModel, dialogProvider)
        }

        mainRobot {
            clickAddFile()
        }
        waitForIdle()
        onNodeWithText("Dashboard").performClick()
        waitForIdle()
    }

    @Test
    fun givenDashboardOpened_whenRenderingAnalysisSections_thenScopeHelpersAndActionHierarchyAreVisible() =
        runComposeUiTest {
            setupApp()

            onNodeWithText("Analysis scope: current filtered logs").assertExists()
            onNodeWithText("Analyze field").assertExists()
            onNodeWithText("Baseline window (reference)").assertExists()
            onNodeWithText("Comparison window (new period)").assertExists()
            onNodeWithText("Leave From or To empty for an open-ended range.").assertExists()
            onNodeWithText("Level distribution").assertDoesNotExist()
            onNodeWithTag("dashboard_level_distribution_chart").assertDoesNotExist()

            onNodeWithTag("run_comparison_button").assertExists().assertIsNotEnabled()
            onNodeWithTag("clear_comparison_button").assertExists().assertIsEnabled()
        }

    @Test
    fun givenDashboardWithRawLevels_whenRenderingSummary_thenLevelDistributionChartIsVisible() =
        runComposeUiTest {
            setupApp(testEntriesWithRawLevel)

            waitUntilAtLeastOneExists(hasText("Level distribution"), 5_000)
            waitUntilAtLeastOneExists(hasTestTag("dashboard_level_row_error"), 5_000)
            onNodeWithText("Level distribution").assertExists()
            onNodeWithTag("dashboard_level_row_error").assertExists()
        }

    @Test
    fun givenDashboardWithOnlyUnknownRawLevels_whenRenderingSummary_thenLevelDistributionChartIsHidden() =
        runComposeUiTest {
            setupApp(testEntriesWithUnknownRawLevel)

            onNodeWithText("Level distribution").assertDoesNotExist()
            onNodeWithTag("dashboard_level_distribution_chart").assertDoesNotExist()
        }

    @Test
    fun givenInvalidBaselineRange_whenEditingComparisonInputs_thenInlineValidationAndGuidanceAreShown() = runComposeUiTest {
        setupApp()

        onNodeWithTag("compare_baseline_from_input").performTextInput("2026-01-01T00:00:02Z")
        onNodeWithTag("compare_baseline_to_input").performTextInput("2026-01-01T00:00:01Z")
        onNodeWithTag("compare_comparison_from_input").performTextInput("2026-01-01T00:00:00Z")
        onNodeWithTag("compare_comparison_to_input").performTextInput("2026-01-01T00:00:01Z")

        onNodeWithText("From must be before or equal to To").assertExists()
        onNodeWithText("Fix the highlighted date/time input to run comparison.").assertExists()
        onNodeWithTag("run_comparison_button").assertIsNotEnabled()
    }

    @Test
    fun givenComparisonRun_whenDeltasRender_thenDirectionTextAndAccessibilityLabelsArePresent() = runComposeUiTest {
        setupApp()

        onNodeWithTag("frequency_field_service").performClick()
        onNodeWithTag("compare_baseline_from_input").performTextInput("2026-01-01T00:00:00Z")
        onNodeWithTag("compare_baseline_to_input").performTextInput("2026-01-01T00:00:00Z")
        onNodeWithTag("compare_comparison_from_input").performTextInput("2026-01-01T00:00:01Z")
        onNodeWithTag("compare_comparison_to_input").performTextInput("2026-01-01T00:00:02Z")
        onNodeWithTag("run_comparison_button").performClick()

        waitForIdle()
        onNodeWithText("Direction legend: ↑ increase, ↓ decrease, = unchanged").assertExists()
        onNodeWithContentDescription("Increase Top N").assertExists()
        onNodeWithContentDescription("Decrease Threshold").assertExists()
        onNodeWithContentDescription("Increase Cardinality limit").assertExists()
    }
}
