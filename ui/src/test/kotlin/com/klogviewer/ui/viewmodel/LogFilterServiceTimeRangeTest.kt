package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TimeRangePreset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import java.time.Instant

class LogFilterServiceTimeRangeTest {

    @Test
    fun `filters logs by inclusive from and to instants`() = runTest {
        val logs = listOf(
            logEntry("2026-05-26T10:00:00Z", "before"),
            logEntry("2026-05-26T10:02:00Z", "in-range-start"),
            logEntry("2026-05-26T10:04:00Z", "in-range-end"),
            logEntry("2026-05-26T10:05:00Z", "after")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterFrom = "2026-05-26T10:02:00Z",
            timeFilterTo = "2026-05-26T10:04:00Z",
            timeFilterFromInstant = Instant.parse("2026-05-26T10:02:00Z"),
            timeFilterToInstant = Instant.parse("2026-05-26T10:04:00Z")
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly("in-range-start", "in-range-end")
    }

    @Test
    fun `filters logs by last five minutes preset using current time as end`() = runTest {
        val now = Instant.now()
        val logs = listOf(
            logEntry(now.minusSeconds(7 * 60), "outside"),
            logEntry(now.minusSeconds(4 * 60), "inside-older"),
            logEntry(now.minusSeconds(45), "inside-recent"),
            logEntry(now.plusSeconds(30), "future")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterPreset = TimeRangePreset.LAST_5_MINUTES
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly("inside-older", "inside-recent")
    }

    @Test
    fun `filters logs by last five minutes preset returns empty when no recent logs exist`() = runTest {
        val now = Instant.now()
        val logs = listOf(
            logEntry(now.minusSeconds(15 * 60), "stale-1"),
            logEntry(now.minusSeconds(9 * 60), "stale-2")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterPreset = TimeRangePreset.LAST_5_MINUTES
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly()
    }

    @Test
    fun `filters logs by last one hour preset using current time as end`() = runTest {
        val now = Instant.now()
        val logs = listOf(
            logEntry(now.minusSeconds(2 * 60 * 60), "outside"),
            logEntry(now.minusSeconds(45 * 60), "inside-start"),
            logEntry(now.minusSeconds(30 * 60), "inside-middle"),
            logEntry(now.minusSeconds(2 * 60), "inside-end"),
            logEntry(now.plusSeconds(60), "future")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterPreset = TimeRangePreset.LAST_1_HOUR
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly("inside-start", "inside-middle", "inside-end")
    }

    private fun logEntry(instant: Instant, content: String): LogEntry = LogEntry(
        timestamp = LogTimestamp(instant.toString()),
        level = LogLevel.INFO,
        content = LogContent(content),
        instant = instant
    )

    private fun logEntry(isoInstant: String, content: String): LogEntry {
        return logEntry(Instant.parse(isoInstant), content)
    }
}
