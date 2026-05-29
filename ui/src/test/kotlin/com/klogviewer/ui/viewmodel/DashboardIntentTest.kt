package com.klogviewer.ui.viewmodel

import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.mvi.DashboardDataState
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.TimeRangePreset
import com.klogviewer.ui.mvi.WorkspaceMode
import com.klogviewer.ui.mvi.DashboardDeltaDirection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardIntentTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var viewModel: KLogViewerViewModel
    private lateinit var logSource: LogSource
    private lateinit var prefsRepository: JsonPreferencesRepository

    @BeforeEach
    fun setup() {
        val testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        prefsRepository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        logSource = mockk()
        every { logSource.observeLogs(any(), any()) } returns flowOf(
            LogUpdate.Initial(
                entries = listOf(
                    logEntry("2026-01-01T00:00:00Z", "first"),
                    logEntry("2026-01-01T00:00:01Z", "second")
                )
            ).right()
        )

        viewModel = KLogViewerViewModel(
            logSource = logSource,
            prefsRepository = prefsRepository,
            heuristicProbe = HeuristicProbe(ParserRegistry()),
            scope = CoroutineScope(testDispatcher),
            dashboardRecomputeDebounceMs = 0L,
            dashboardSamplingThreshold = 6,
            dashboardSamplingTargetSize = 3
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.clear()
    }

    @Test
    fun `given active window when applying time filter preset then preset is stored`() {
        val testFile = File(tempDir, "time-filter.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ApplyTimeFilterPreset(TimeRangePreset.LAST_5_MINUTES))

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.timeFilterPreset).isEqualTo(TimeRangePreset.LAST_5_MINUTES)
        expectThat(window.timeFilterFrom.isNotBlank()).isTrue()
        expectThat(window.timeFilterTo.isNotBlank()).isTrue()
    }

    @Test
    fun `given active window when applying full loaded range preset then range matches loaded timestamps`() {
        val testFile = File(tempDir, "full-range.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ApplyTimeFilterPreset(TimeRangePreset.FULL_LOADED_RANGE))

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.timeFilterPreset).isEqualTo(TimeRangePreset.FULL_LOADED_RANGE)
        expectThat(window.timeFilterFrom).isEqualTo("2026-01-01T00:00:00Z")
        expectThat(window.timeFilterTo).isEqualTo("2026-01-01T00:00:01Z")
    }

    @Test
    fun `given first tab has time filter when switching tabs then time filter state remains isolated per tab`() {
        val testFile = File(tempDir, "tab-isolation.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        val firstTabId = requireNotNull(viewModel.state.value.activeTabId)
        viewModel.handleIntent(KLogViewerIntent.SetTimeFilterFrom("2026-01-01T00:00:00Z"))

        viewModel.handleIntent(KLogViewerIntent.AddTab)
        val secondTabWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(secondTabWindow.timeFilterFrom).isEqualTo("")
        expectThat(secondTabWindow.timeFilterPreset).isNull()

        viewModel.handleIntent(KLogViewerIntent.SwitchTab(firstTabId))
        val firstTabWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(firstTabWindow.timeFilterFrom).isEqualTo("2026-01-01T00:00:00Z")
    }

    @Test
    fun `given active window when showing dashboard then dashboard content becomes available`() {
        val testFile = File(tempDir, "dashboard-mode.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.workspaceMode).isEqualTo(WorkspaceMode.DASHBOARD)
        expectThat(window.dashboardDataState is DashboardDataState.Content).isTrue()
    }

    @Test
    fun `given dashboard content when selecting level then level selection toggles filter`() {
        val testFile = File(tempDir, "dashboard-level.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        viewModel.handleIntent(KLogViewerIntent.SelectDashboardLevel(LogLevel.INFO))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val content = activeWindow?.dashboardDataState as? DashboardDataState.Content
            activeWindow?.levelFilters == setOf(LogLevel.INFO) && content?.selectedLevel == LogLevel.INFO
        }

        viewModel.handleIntent(KLogViewerIntent.ClearDashboardSelections)

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val content = activeWindow?.dashboardDataState as? DashboardDataState.Content
            activeWindow?.levelFilters == LogLevel.entries.toSet() && content?.selectedLevel == null
        }
    }

    @Test
    fun `given dashboard content when selecting time bucket then time range toggles`() {
        val testFile = File(tempDir, "dashboard-bucket.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val selectedBucket = activeDashboardContent().timeSeries.first()
        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeBucket(selectedBucket.from))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.timeFilterFrom == selectedBucket.from.toString() &&
                activeWindow.timeFilterTo == selectedBucket.to.toString()
        }

        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeBucket(selectedBucket.from))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.timeFilterFrom == "" && activeWindow.timeFilterTo == ""
        }
    }

    @Test
    fun `given dashboard content when selecting explicit time range then custom range is synchronized`() {
        val testFile = File(tempDir, "dashboard-range.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val selectedBucket = activeDashboardContent().timeSeries.first()
        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(selectedBucket.from, selectedBucket.to))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.timeFilterFrom == selectedBucket.from.toString() &&
                activeWindow.timeFilterTo == selectedBucket.to.toString() &&
                activeWindow.timeFilterPreset == TimeRangePreset.CUSTOM
        }
    }

    @Test
    fun `given dashboard content when rendered then level distribution includes every normalized level`() {
        val testFile = File(tempDir, "dashboard-level-distribution.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val content = activeDashboardContent()
        expectThat(content.levelDistribution.map { it.level }).isEqualTo(LogLevel.entries.toList())
        expectThat(content.levelDistribution.sumOf { it.count }).isEqualTo(content.totalEvents)
    }

    @Test
    fun `given unknown level entries when rendering dashboard then unknown level slice reflects unknown count`() {
        reconfigureLogSource(
            listOf(
                logEntry("2026-01-01T00:00:00Z", "known", level = LogLevel.INFO),
                logEntry("2026-01-01T00:00:01Z", "unknown-1", level = LogLevel.UNKNOWN),
                logEntry("2026-01-01T00:00:02Z", "unknown-2", level = LogLevel.UNKNOWN)
            )
        )

        val testFile = File(tempDir, "dashboard-unknown-level.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val unknownSlice = activeDashboardContent().levelDistribution.first { it.level == LogLevel.UNKNOWN }
        expectThat(unknownSlice.count).isEqualTo(2)
    }

    @Test
    fun `given frequency field selection when selecting value then dashboard filter query toggles`() {
        reconfigureLogSource(
            listOf(
                logEntry("2026-01-01T00:00:00Z", "first", fields = mapOf("service" to "auth")),
                logEntry("2026-01-01T00:00:01Z", "second", fields = mapOf("service" to "auth")),
                logEntry("2026-01-01T00:00:02Z", "third", fields = mapOf("service" to "billing"))
            )
        )

        val testFile = File(tempDir, "dashboard-frequency-toggle.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))

        waitUntil {
            activeDashboardContent().selectedFrequencyField == "service"
        }

        val selectedValue = "auth"
        viewModel.handleIntent(KLogViewerIntent.SelectDashboardFrequencyValue(selectedValue))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val content = activeWindow?.dashboardDataState as? DashboardDataState.Content
            activeWindow?.filterQueries?.contains("@field:service=$selectedValue") == true &&
                content?.selectedFrequencyValue == selectedValue
        }

        viewModel.handleIntent(KLogViewerIntent.SelectDashboardFrequencyValue(selectedValue))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val content = activeWindow?.dashboardDataState as? DashboardDataState.Content
            activeWindow?.filterQueries?.none { it.startsWith("@field:") } == true &&
                content?.selectedFrequencyValue == null
        }
    }

    @Test
    fun `given missing structured values when selecting field then missing bucket is included`() {
        reconfigureLogSource(
            listOf(
                logEntry("2026-01-01T00:00:00Z", "first", fields = mapOf("service" to "auth")),
                logEntry("2026-01-01T00:00:01Z", "second"),
                logEntry("2026-01-01T00:00:02Z", "third")
            )
        )

        val testFile = File(tempDir, "dashboard-frequency-missing.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))

        waitUntil {
            activeDashboardContent().frequencyItems.any { it.value == "(missing)" }
        }
    }

    @Test
    fun `given top n and threshold controls when applying frequency analysis then ordering remains deterministic`() {
        reconfigureLogSource(
            listOf(
                logEntry("2026-01-01T00:00:00Z", "first", fields = mapOf("team" to "a")),
                logEntry("2026-01-01T00:00:01Z", "second", fields = mapOf("team" to "b")),
                logEntry("2026-01-01T00:00:02Z", "third", fields = mapOf("team" to "a")),
                logEntry("2026-01-01T00:00:03Z", "fourth", fields = mapOf("team" to "b")),
                logEntry("2026-01-01T00:00:04Z", "fifth", fields = mapOf("team" to "c"))
            )
        )

        val testFile = File(tempDir, "dashboard-frequency-controls.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("team"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyThreshold(2))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyTopN(1))

        waitUntil {
            activeDashboardContent().frequencyItems.map { it.value } == listOf("a")
        }
    }

    @Test
    fun `given compare ranges when running comparison then level and field deltas are produced`() {
        reconfigureLogSource(
            listOf(
                logEntry(
                    "2026-01-01T00:00:00Z",
                    "first",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:01Z",
                    "second",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                ),
                logEntry(
                    "2026-01-01T00:00:02Z",
                    "third",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                )
            )
        )

        val testFile = File(tempDir, "dashboard-compare.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo("2026-01-01T00:00:02Z"))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        waitUntil {
            val comparisonState = activeDashboardContent().comparisonState
            comparisonState.levelDeltas.any { it.delta != 0 } &&
                comparisonState.fieldDeltas.any { (it.delta ?: 0) != 0 }
        }
    }

    @Test
    fun `given compare ranges when running comparison then deltas are correct and sorted by impact`() {
        reconfigureLogSource(
            listOf(
                logEntry(
                    "2026-01-01T00:00:00Z",
                    "baseline-a",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:01Z",
                    "baseline-b",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "billing")
                ),
                logEntry(
                    "2026-01-01T00:00:10Z",
                    "comparison-a",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:11Z",
                    "comparison-b",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:12Z",
                    "comparison-c",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:13Z",
                    "comparison-d",
                    level = LogLevel.WARN,
                    fields = mapOf("service" to "db")
                )
            )
        )

        val testFile = File(tempDir, "dashboard-compare-ordering.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom("2026-01-01T00:00:10Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo("2026-01-01T00:00:13Z"))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        waitUntil {
            val comparisonState = activeDashboardContent().comparisonState
            comparisonState.fieldDeltas.isNotEmpty() && comparisonState.levelDeltas.any { it.delta != 0 }
        }

        val comparisonState = activeDashboardContent().comparisonState
        val infoDelta = comparisonState.levelDeltas.first { it.level == LogLevel.INFO }
        val errorDelta = comparisonState.levelDeltas.first { it.level == LogLevel.ERROR }
        val warnDelta = comparisonState.levelDeltas.first { it.level == LogLevel.WARN }
        expectThat(infoDelta.delta).isEqualTo(-1)
        expectThat(infoDelta.direction).isEqualTo(DashboardDeltaDirection.DECREASE)
        expectThat(errorDelta.delta).isEqualTo(2)
        expectThat(errorDelta.direction).isEqualTo(DashboardDeltaDirection.INCREASE)
        expectThat(warnDelta.delta).isEqualTo(1)

        expectThat(comparisonState.fieldDeltas.map { it.value }).isEqualTo(listOf("auth", "db", "billing"))
        expectThat(comparisonState.fieldDeltas.map { it.delta }).isEqualTo(listOf(2, 1, -1))
    }

    @Test
    fun `given comparison already computed when editing ranges then deltas are cleared until explicit rerun`() {
        reconfigureLogSource(
            listOf(
                logEntry(
                    "2026-01-01T00:00:00Z",
                    "first",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:01Z",
                    "second",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                ),
                logEntry(
                    "2026-01-01T00:00:02Z",
                    "third",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                )
            )
        )

        val testFile = File(tempDir, "dashboard-compare-manual-run.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo("2026-01-01T00:00:02Z"))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        waitUntil {
            activeDashboardContent().comparisonState.levelDeltas.any { it.delta != 0 }
        }

        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:01Z"))

        val comparisonState = activeDashboardContent().comparisonState
        expectThat(comparisonState.levelDeltas).isEqualTo(emptyList())
        expectThat(comparisonState.fieldDeltas).isEqualTo(emptyList())
    }

    @Test
    fun `given comparison field deltas when frequency threshold changes then rerun uses updated controls`() {
        reconfigureLogSource(
            listOf(
                logEntry(
                    "2026-01-01T00:00:00Z",
                    "first",
                    level = LogLevel.INFO,
                    fields = mapOf("service" to "auth")
                ),
                logEntry(
                    "2026-01-01T00:00:01Z",
                    "second",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                ),
                logEntry(
                    "2026-01-01T00:00:02Z",
                    "third",
                    level = LogLevel.ERROR,
                    fields = mapOf("service" to "billing")
                )
            )
        )

        val testFile = File(tempDir, "dashboard-compare-coupled-controls.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField("service"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyThreshold(1))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo("2026-01-01T00:00:02Z"))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        waitUntil {
            activeDashboardContent().comparisonState.fieldDeltas.map { it.value }.containsAll(listOf("auth", "billing"))
        }

        viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyThreshold(2))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        waitUntil {
            activeDashboardContent().comparisonState.fieldDeltas.map { it.value } == listOf("billing")
        }
    }

    @Test
    fun `given invalid compare range when running comparison then deltas remain empty`() {
        val testFile = File(tempDir, "dashboard-compare-invalid.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom("2026-01-01T00:00:02Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom("2026-01-01T00:00:00Z"))
        viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo("2026-01-01T00:00:01Z"))
        viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison)

        val comparisonState = activeDashboardContent().comparisonState
        expectThat(comparisonState.levelDeltas).isEqualTo(emptyList())
        expectThat(comparisonState.fieldDeltas).isEqualTo(emptyList())
    }

    @Test
    fun `given logs when showing dashboard then time series buckets are sorted chronologically`() {
        reconfigureLogSource(
            listOf(
                logEntry("2026-01-01T00:00:10Z", "late"),
                logEntry("2026-01-01T00:00:00Z", "early"),
                logEntry("2026-01-01T00:00:05Z", "middle")
            )
        )

        val testFile = File(tempDir, "sorted-dashboard.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val content = activeDashboardContent()
        val timestamps = content.timeSeries.map { it.from }
        expectThat(timestamps).isEqualTo(timestamps.sorted())
    }

    @Test
    fun `given dashboard content when selecting time bucket then time filter is applied and selection is synchronized`() {
        val entry1 = logEntry("2026-01-01T00:00:00Z", "early")
        val entry2 = logEntry("2026-01-01T00:01:00Z", "late")
        reconfigureLogSource(listOf(entry1, entry2))

        val testFile = File(tempDir, "dashboard-select.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val content = activeDashboardContent()
        val firstBucket = content.timeSeries.first()

        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(firstBucket.from, firstBucket.to))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val selectedBucketFrom = (activeWindow?.dashboardDataState as? DashboardDataState.Content)
                ?.selectedBucketFrom
            activeWindow?.timeFilterFromInstant == firstBucket.from &&
                activeWindow.timeFilterToInstant == firstBucket.to &&
                selectedBucketFrom == firstBucket.from
        }

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.timeFilterFromInstant).isEqualTo(firstBucket.from)
        expectThat(window.timeFilterToInstant).isEqualTo(firstBucket.to)
    }

    @Test
    fun `given dashboard time range selection when restoring from preferences then selected range is remembered`() {
        val entry1 = logEntry("2026-01-01T00:00:00Z", "early")
        val entry2 = logEntry("2026-01-01T00:01:00Z", "late")
        reconfigureLogSource(listOf(entry1, entry2))

        val testFile = File(tempDir, "dashboard-time-filter-persistence.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val firstBucket = activeDashboardContent().timeSeries.first()
        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(firstBucket.from, firstBucket.to))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.timeFilterFromInstant == firstBucket.from &&
                activeWindow.timeFilterToInstant == firstBucket.to
        }

        val restoredViewModel = KLogViewerViewModel(
            logSource = logSource,
            prefsRepository = prefsRepository,
            heuristicProbe = HeuristicProbe(ParserRegistry())
        )

        try {
            val restoredWindow = requireNotNull(restoredViewModel.state.value.activeTab?.activeWindow)
            expectThat(restoredWindow.timeFilterFrom).isEqualTo(firstBucket.from.toString())
            expectThat(restoredWindow.timeFilterTo).isEqualTo(firstBucket.to.toString())
            expectThat(restoredWindow.timeFilterFromInstant).isEqualTo(firstBucket.from)
            expectThat(restoredWindow.timeFilterToInstant).isEqualTo(firstBucket.to)
        } finally {
            restoredViewModel.clear()
        }
    }

    @Test
    fun `given dashboard content when selecting time range across buckets then combined filter is applied`() {
        val entry1 = logEntry("2026-01-01T00:00:00Z", "early")
        val entry2 = logEntry("2026-01-01T00:01:00Z", "middle")
        val entry3 = logEntry("2026-01-01T00:02:00Z", "late")
        reconfigureLogSource(listOf(entry1, entry2, entry3))

        val testFile = File(tempDir, "dashboard-range-select.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val content = activeDashboardContent()
        val firstBucket = content.timeSeries.first()
        val lastBucket = content.timeSeries.last()

        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(firstBucket.from, lastBucket.to))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val selectedBucketFrom = (activeWindow?.dashboardDataState as? DashboardDataState.Content)
                ?.selectedBucketFrom
            activeWindow?.timeFilterFromInstant == firstBucket.from &&
                activeWindow.timeFilterToInstant == lastBucket.to &&
                selectedBucketFrom == null
        }

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.timeFilterFromInstant).isEqualTo(firstBucket.from)
        expectThat(window.timeFilterToInstant).isEqualTo(lastBucket.to)
    }

    @Test
    fun `given selected dashboard time bucket when clearing selections then chart applied filter is removed`() {
        val entry1 = logEntry("2026-01-01T00:00:00Z", "early")
        val entry2 = logEntry("2026-01-01T00:01:00Z", "late")
        reconfigureLogSource(listOf(entry1, entry2))

        val testFile = File(tempDir, "dashboard-clear-selection.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val firstBucket = activeDashboardContent().timeSeries.first()
        viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(firstBucket.from, firstBucket.to))

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val selectedBucketFrom = (activeWindow?.dashboardDataState as? DashboardDataState.Content)
                ?.selectedBucketFrom
            selectedBucketFrom == firstBucket.from
        }

        viewModel.handleIntent(KLogViewerIntent.ClearDashboardSelections)

        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            val selectedBucketFrom = (activeWindow?.dashboardDataState as? DashboardDataState.Content)
                ?.selectedBucketFrom
            activeWindow?.timeFilterFrom.isNullOrEmpty() &&
                activeWindow?.timeFilterTo.isNullOrEmpty() &&
                selectedBucketFrom == null
        }
    }

    @Test
    fun `given high-volume append updates when dashboard is open then content remains responsive`() {
        val initialEntries = (0 until 200).map { index ->
            logEntry(
                timestamp = "2026-01-01T00:00:${(index % 60).toString().padStart(2, '0')}Z",
                content = "initial-$index",
                fields = mapOf("service" to "svc-${index % 8}")
            )
        }
        val appendedEntries = (200 until 400).map { index ->
            logEntry(
                timestamp = "2026-01-01T00:01:${(index % 60).toString().padStart(2, '0')}Z",
                content = "append-$index",
                fields = mapOf("service" to "svc-${index % 8}")
            )
        }

        every { logSource.observeLogs(any(), any()) } returns flowOf(
            LogUpdate.Initial(entries = initialEntries).right(),
            LogUpdate.Appended(entries = appendedEntries).right()
        )

        val testFile = File(tempDir, "dashboard-high-volume.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.logs?.size == 400 && activeWindow.filteredLogs.size == 400
        }

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        waitUntilDashboardContentReady()

        val content = activeDashboardContent()
        expectThat(content.totalEvents).isEqualTo(400)
        expectThat(content.samplingInfo.mode.name).isEqualTo("DETERMINISTIC")
    }

    @Test
    fun `given large filtered dataset when dashboard is shown then deterministic sampling metadata is reported`() {
        val largeEntries = (0 until 12).map { index ->
            logEntry(
                timestamp = "2026-01-01T00:00:${index.toString().padStart(2, '0')}Z",
                content = "entry-$index",
                fields = mapOf("service" to "svc-${index % 4}")
            )
        }
        reconfigureLogSource(largeEntries)

        val testFile = File(tempDir, "dashboard-sampling.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard)
        Thread.sleep(25)

        waitUntilDashboardContentReady()
        val content = activeDashboardContent()
        expectThat(content.samplingInfo.mode.name).isEqualTo("DETERMINISTIC")
        expectThat(content.samplingInfo.originalCount).isEqualTo(12)
        expectThat(content.samplingInfo.sampledCount).isEqualTo(3)
    }

    private fun waitUntil(timeoutMillis: Long = 5_000, pollIntervalMillis: Long = 10, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(pollIntervalMillis)
        }
        throw AssertionError("Condition was not met in time")
    }

    private fun waitUntilWindowReady() {
        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.logs?.isNotEmpty() == true && activeWindow.filteredLogs.isNotEmpty()
        }
    }

    private fun waitUntilDashboardContentReady() {
        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.dashboardDataState is DashboardDataState.Content
        }
    }

    private fun activeDashboardContent(): DashboardDataState.Content {
        val activeWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        val content = activeWindow.dashboardDataState as? DashboardDataState.Content
        expectThat(content).isNotNull()
        return requireNotNull(content)
    }

    private fun reconfigureLogSource(entries: List<LogEntry>) {
        every { logSource.observeLogs(any(), any()) } returns flowOf(
            LogUpdate.Initial(entries = entries).right()
        )
    }

    private fun logEntry(
        timestamp: String,
        content: String,
        level: LogLevel = LogLevel.INFO,
        fields: Map<String, String> = emptyMap()
    ): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = level,
            content = LogContent(content),
            fields = fields,
            instant = Instant.parse(timestamp)
        )
    }
}
