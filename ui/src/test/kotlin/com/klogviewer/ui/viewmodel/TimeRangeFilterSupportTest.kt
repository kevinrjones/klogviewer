package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TimeRangePreset
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.time.Instant

class TimeRangeFilterSupportTest {

    @Test
    fun `parseInstantOrNull supports common timestamp formats`() {
        val isoInstant = TimeRangeFilterSupport.parseInstantOrNull("2026-05-26T10:00:00Z")
        val slashFormat = TimeRangeFilterSupport.parseInstantOrNull("2026/05/26 10:00:00")
        val commaMillis = TimeRangeFilterSupport.parseInstantOrNull("2026-05-26 10:00:00,123")
        val apacheFormat = TimeRangeFilterSupport.parseInstantOrNull("26/May/2026:10:00:00 +0000")
        val epochSeconds = TimeRangeFilterSupport.parseInstantOrNull("1780000000")
        val epochMillis = TimeRangeFilterSupport.parseInstantOrNull("1780000000000")

        expectThat(isoInstant).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"))
        expectThat(slashFormat).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"))
        expectThat(commaMillis).isEqualTo(Instant.parse("2026-05-26T10:00:00.123Z"))
        expectThat(apacheFormat).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"))
        expectThat(epochSeconds).isEqualTo(Instant.ofEpochSecond(1_780_000_000L))
        expectThat(epochMillis).isEqualTo(Instant.ofEpochMilli(1_780_000_000_000L))
    }

    @Test
    fun `validationMessage rejects invalid and inverted ranges`() {
        val invalidFrom = TimeRangeFilterSupport.validationMessage(
            fromValue = "not-a-date",
            fromInstant = null,
            toValue = "",
            toInstant = null
        )

        val invalidTo = TimeRangeFilterSupport.validationMessage(
            fromValue = "",
            fromInstant = null,
            toValue = "still-not-a-date",
            toInstant = null
        )

        val inverted = TimeRangeFilterSupport.validationMessage(
            fromValue = "2026-05-26T10:00:01Z",
            fromInstant = Instant.parse("2026-05-26T10:00:01Z"),
            toValue = "2026-05-26T10:00:00Z",
            toInstant = Instant.parse("2026-05-26T10:00:00Z")
        )

        val valid = TimeRangeFilterSupport.validationMessage(
            fromValue = "2026-05-26T10:00:00Z",
            fromInstant = Instant.parse("2026-05-26T10:00:00Z"),
            toValue = "2026-05-26T10:01:00Z",
            toInstant = Instant.parse("2026-05-26T10:01:00Z")
        )

        expectThat(invalidFrom).isEqualTo("Could not parse 'From' date/time")
        expectThat(invalidTo).isEqualTo("Could not parse 'To' date/time")
        expectThat(inverted).isEqualTo("From must be before or equal to To")
        expectThat(valid).isNull()
    }

    @Test
    fun `resolveRange uses latest log instant for last five minutes preset`() {
        val window = LogWindow(
            id = "window-1",
            logs = listOf(
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            ),
            timeFilterPreset = TimeRangePreset.LAST_5_MINUTES
        )

        val range = TimeRangeFilterSupport.resolveRange(window)

        expectThat(range).isNotNull()
        expectThat(range!!.first).isEqualTo(Instant.parse("2026-05-26T09:58:00Z"))
        expectThat(range.second).isEqualTo(Instant.parse("2026-05-26T10:03:00Z"))
    }

    private fun logEntry(isoInstant: String): LogEntry = LogEntry(
        timestamp = LogTimestamp(isoInstant),
        level = LogLevel.INFO,
        content = LogContent("message-$isoInstant"),
        instant = Instant.parse(isoInstant)
    )
}
