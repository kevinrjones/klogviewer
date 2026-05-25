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
import com.klogviewer.ui.mvi.DashboardUiState
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.WindowViewMode
import io.mockk.every
import io.mockk.mockk
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
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardIntentTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var viewModel: KLogViewerViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val prefsRepository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
        val logSource = mockk<LogSource>()
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
            heuristicProbe = HeuristicProbe(ParserRegistry())
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.clear()
    }

    @Test
    fun `given active log window when show dashboard intent then dashboard content state is populated`() {
        val testFile = File(tempDir, "dashboard.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ShowDashboard())

        waitUntil {
            viewModel.state.value.activeTab?.activeWindow?.dashboardState is DashboardUiState.Content
        }
        val window = viewModel.state.value.activeTab?.activeWindow
        expectThat(window).isNotNull()
        expectThat(window?.viewMode).isEqualTo(WindowViewMode.DASHBOARD)
        expectThat(window?.dashboardState).isA<DashboardUiState.Content>()
    }

    @Test
    fun `given dashboard content when bucket selected then existing log filtering is applied and can be cleared`() {
        val testFile = File(tempDir, "bucket.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()
        viewModel.handleIntent(KLogViewerIntent.ShowDashboard())
        waitUntil {
            viewModel.state.value.activeTab?.activeWindow?.dashboardState is DashboardUiState.Content
        }

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        val dashboardState = window.dashboardState as DashboardUiState.Content
        val bucket = dashboardState.buckets.first()

        viewModel.handleIntent(
            KLogViewerIntent.SelectDashboardBucket(
                windowId = window.id,
                from = bucket.from,
                to = bucket.to,
                timestampFilter = bucket.timestampFilter
            )
        )

        waitUntil {
            viewModel.state.value.activeTab?.activeWindow?.dashboardFilterQuery == bucket.timestampFilter
        }
        val filteredWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(filteredWindow.dashboardFilterQuery).isEqualTo(bucket.timestampFilter)
        expectThat(filteredWindow.filteredLogs.isNotEmpty()).isTrue()

        viewModel.handleIntent(KLogViewerIntent.ClearDashboardBucketFilter(filteredWindow.id))
        waitUntil {
            viewModel.state.value.activeTab?.activeWindow?.dashboardFilterQuery == null
        }
        expectThat(viewModel.state.value.activeTab?.activeWindow?.dashboardFilterQuery).isEqualTo(null)
    }

    @Test
    fun `given dashboard opened on one tab when switching tabs then dashboard state remains isolated per tab`() {
        val testFile = File(tempDir, "isolation.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        val firstTabId = requireNotNull(viewModel.state.value.activeTabId)
        viewModel.handleIntent(KLogViewerIntent.ShowDashboard())
        waitUntil {
            viewModel.state.value.activeTab?.activeWindow?.viewMode == WindowViewMode.DASHBOARD
        }

        viewModel.handleIntent(KLogViewerIntent.AddTab)
        val secondTabWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(secondTabWindow.viewMode).isEqualTo(WindowViewMode.LOGS)
        expectThat(secondTabWindow.dashboardFilterQuery != null).isFalse()

        viewModel.handleIntent(KLogViewerIntent.SwitchTab(firstTabId))
        val firstTabWindow = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(firstTabWindow.viewMode).isEqualTo(WindowViewMode.DASHBOARD)
    }

    private fun waitUntil(maxAttempts: Int = 200, predicate: () -> Boolean) {
        repeat(maxAttempts) {
            if (predicate()) return
            Thread.sleep(10)
        }
        throw AssertionError("Condition was not met in time")
    }

    private fun waitUntilWindowReady() {
        waitUntil {
            val activeWindow = viewModel.state.value.activeTab?.activeWindow
            activeWindow?.logs?.isNotEmpty() == true && activeWindow.filteredLogs.isNotEmpty()
        }
    }

    private fun logEntry(timestamp: String, content: String): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = LogLevel.INFO,
            content = LogContent(content),
            instant = Instant.parse(timestamp)
        )
    }
}
