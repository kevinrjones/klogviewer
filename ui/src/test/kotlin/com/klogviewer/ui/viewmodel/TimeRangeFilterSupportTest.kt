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
    fun `resolveRange uses current time for last five minutes preset`() {
        val now = Instant.parse("2026-05-26T10:10:00Z")
        val window = LogWindow(
            id = "window-1",
            logs = listOf(
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            ),
            timeFilterPreset = TimeRangePreset.LAST_5_MINUTES
        )

        val range = TimeRangeFilterSupport.resolveRange(window, now)

        expectThat(range).isNotNull()
        expectThat(range!!.first).isEqualTo(Instant.parse("2026-05-26T10:05:00Z"))
        expectThat(range.second).isEqualTo(Instant.parse("2026-05-26T10:10:00Z"))
    }

    @Test
    fun `resolveRange uses current time for longer presets`() {
        val now = Instant.parse("2026-05-26T10:10:00Z")
        val window = LogWindow(
            id = "window-1",
            logs = listOf(
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            ),
            timeFilterPreset = TimeRangePreset.LAST_1_HOUR
        )

        val range = TimeRangeFilterSupport.resolveRange(window, now)

        expectThat(range).isNotNull()
        expectThat(range!!.first).isEqualTo(Instant.parse("2026-05-26T09:10:00Z"))
        expectThat(range.second).isEqualTo(Instant.parse("2026-05-26T10:10:00Z"))
    }

    @Test
    fun `resolveRange uses explicit from and to values for custom preset`() {
        val window = LogWindow(
            id = "window-1",
            logs = listOf(
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            ),
            timeFilterFrom = "2026-05-26T10:00:00Z",
            timeFilterTo = "2026-05-26T10:03:00Z",
            timeFilterFromInstant = Instant.parse("2026-05-26T10:00:00Z"),
            timeFilterToInstant = Instant.parse("2026-05-26T10:03:00Z"),
            timeFilterPreset = TimeRangePreset.CUSTOM
        )

        val range = TimeRangeFilterSupport.resolveRange(window)

        expectThat(range).isNotNull()
        expectThat(range!!.first).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"))
        expectThat(range.second).isEqualTo(Instant.parse("2026-05-26T10:03:00Z"))
    }

    @Test
    fun `resolvePresetSelection supports visible and full loaded ranges`() {
        val window = LogWindow(
            id = "window-1",
            logs = listOf(
                logEntry("2026-05-26T09:58:00Z"),
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            ),
            filteredLogs = listOf(
                logEntry("2026-05-26T10:00:00Z"),
                logEntry("2026-05-26T10:03:00Z")
            )
        )

        val visibleRange = TimeRangeFilterSupport.resolvePresetSelection(window, TimeRangePreset.VISIBLE_WINDOW)
        val fullRange = TimeRangeFilterSupport.resolvePresetSelection(window, TimeRangePreset.FULL_LOADED_RANGE)

        expectThat(visibleRange).isNotNull()
        expectThat(visibleRange!!.first).isEqualTo(Instant.parse("2026-05-26T10:00:00Z"))
        expectThat(visibleRange.second).isEqualTo(Instant.parse("2026-05-26T10:03:00Z"))

        expectThat(fullRange).isNotNull()
        expectThat(fullRange!!.first).isEqualTo(Instant.parse("2026-05-26T09:58:00Z"))
        expectThat(fullRange.second).isEqualTo(Instant.parse("2026-05-26T10:03:00Z"))
    }

    @Test
    fun `preset minute mapping supports all time range presets`() {
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_5_MINUTES)).isEqualTo(5L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_15_MINUTES)).isEqualTo(15L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_30_MINUTES)).isEqualTo(30L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_1_HOUR)).isEqualTo(60L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_6_HOURS)).isEqualTo(360L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.LAST_24_HOURS)).isEqualTo(1_440L)
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.VISIBLE_WINDOW)).isNull()
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.FULL_LOADED_RANGE)).isNull()
        expectThat(TimeRangeFilterSupport.toMinutes(TimeRangePreset.CUSTOM)).isNull()

        expectThat(TimeRangeFilterSupport.toPreset(5L)).isEqualTo(TimeRangePreset.LAST_5_MINUTES)
        expectThat(TimeRangeFilterSupport.toPreset(15L)).isEqualTo(TimeRangePreset.LAST_15_MINUTES)
        expectThat(TimeRangeFilterSupport.toPreset(30L)).isEqualTo(TimeRangePreset.LAST_30_MINUTES)
        expectThat(TimeRangeFilterSupport.toPreset(60L)).isEqualTo(TimeRangePreset.LAST_1_HOUR)
        expectThat(TimeRangeFilterSupport.toPreset(360L)).isEqualTo(TimeRangePreset.LAST_6_HOURS)
        expectThat(TimeRangeFilterSupport.toPreset(1_440L)).isEqualTo(TimeRangePreset.LAST_24_HOURS)
        expectThat(TimeRangeFilterSupport.toPreset(2L)).isNull()
    }

    private fun logEntry(isoInstant: String): LogEntry = LogEntry(
        timestamp = LogTimestamp(isoInstant),
        level = LogLevel.INFO,
        content = LogContent("message-$isoInstant"),
        instant = Instant.parse(isoInstant)
    )
}
