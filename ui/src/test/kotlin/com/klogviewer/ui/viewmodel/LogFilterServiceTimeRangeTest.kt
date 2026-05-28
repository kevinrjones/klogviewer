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
    fun `from only includes entries at or after from minus one second`() = runTest {
        val logs = listOf(
            logEntry("2026-05-26T10:00:58Z", "before-tolerance"),
            logEntry("2026-05-26T10:00:59Z", "at-tolerance-start"),
            logEntry("2026-05-26T10:01:00Z", "at-from"),
            logEntry("2026-05-26T10:01:01Z", "after-from")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterFrom = "2026-05-26T10:01:00Z",
            timeFilterFromInstant = Instant.parse("2026-05-26T10:01:00Z")
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly(
            "at-tolerance-start",
            "at-from",
            "after-from"
        )
    }

    @Test
    fun `to only includes entries at or before to plus one second`() = runTest {
        val logs = listOf(
            logEntry("2026-05-26T10:04:59Z", "before-to"),
            logEntry("2026-05-26T10:05:00Z", "at-to"),
            logEntry("2026-05-26T10:05:01Z", "at-tolerance-end"),
            logEntry("2026-05-26T10:05:02Z", "after-tolerance")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterTo = "2026-05-26T10:05:00Z",
            timeFilterToInstant = Instant.parse("2026-05-26T10:05:00Z")
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly(
            "before-to",
            "at-to",
            "at-tolerance-end"
        )
    }

    @Test
    fun `from and to include tolerated closed interval`() = runTest {
        val logs = listOf(
            logEntry("2026-05-26T09:59:58Z", "below-lower-tolerance"),
            logEntry("2026-05-26T09:59:59Z", "at-lower-tolerance"),
            logEntry("2026-05-26T10:00:00Z", "at-from"),
            logEntry("2026-05-26T10:05:00Z", "at-to"),
            logEntry("2026-05-26T10:05:01Z", "at-upper-tolerance"),
            logEntry("2026-05-26T10:05:02Z", "above-upper-tolerance")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterFrom = "2026-05-26T10:00:00Z",
            timeFilterTo = "2026-05-26T10:05:00Z",
            timeFilterFromInstant = Instant.parse("2026-05-26T10:00:00Z"),
            timeFilterToInstant = Instant.parse("2026-05-26T10:05:00Z")
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly(
            "at-lower-tolerance",
            "at-from",
            "at-to",
            "at-upper-tolerance"
        )
    }

    @Test
    fun `entries without timestamps are excluded when time range is active`() = runTest {
        val logs = listOf(
            logEntryWithNoInstant("missing-timestamp"),
            logEntry("2026-05-26T10:00:00Z", "timestamped")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterFrom = "2026-05-26T10:00:00Z",
            timeFilterFromInstant = Instant.parse("2026-05-26T10:00:00Z")
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly("timestamped")
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

    private fun logEntryWithNoInstant(content: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("not-a-timestamp"),
        level = LogLevel.INFO,
        content = LogContent(content),
        instant = null
    )
}
