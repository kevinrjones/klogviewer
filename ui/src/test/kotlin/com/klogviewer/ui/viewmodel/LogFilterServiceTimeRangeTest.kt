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
    fun `filters logs by last five minutes preset using latest log timestamp as end`() = runTest {
        val logs = listOf(
            logEntry("2026-05-26T09:59:59Z", "outside"),
            logEntry("2026-05-26T10:00:00Z", "start"),
            logEntry("2026-05-26T10:04:30Z", "inside"),
            logEntry("2026-05-26T10:05:00Z", "end")
        )

        val window = LogWindow(
            id = "window-1",
            logs = logs,
            timeFilterPreset = TimeRangePreset.LAST_5_MINUTES
        )

        val result = LogFilterService.filter(window)

        expectThat(result.map { it.content.value }).containsExactly("start", "inside", "end")
    }

    private fun logEntry(isoInstant: String, content: String): LogEntry = LogEntry(
        timestamp = LogTimestamp(isoInstant),
        level = LogLevel.INFO,
        content = LogContent(content),
        instant = Instant.parse(isoInstant)
    )
}
