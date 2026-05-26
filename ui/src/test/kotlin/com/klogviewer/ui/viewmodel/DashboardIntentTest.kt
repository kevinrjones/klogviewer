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
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.TimeRangePreset
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
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
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
    fun `given active window when applying time filter preset then preset is stored`() {
        val testFile = File(tempDir, "time-filter.log").apply { writeText("line1\n") }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(testFile.absolutePath)))
        waitUntilWindowReady()

        viewModel.handleIntent(KLogViewerIntent.ApplyTimeFilterPreset(TimeRangePreset.LAST_5_MINUTES))

        val window = requireNotNull(viewModel.state.value.activeTab?.activeWindow)
        expectThat(window.timeFilterPreset).isEqualTo(TimeRangePreset.LAST_5_MINUTES)
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
