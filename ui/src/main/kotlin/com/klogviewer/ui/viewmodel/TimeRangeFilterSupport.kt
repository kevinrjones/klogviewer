package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TimeRangePreset
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.util.Locale

internal object TimeRangeFilterSupport {
    private val dateTimeFormatters = listOf(
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US),
        DateTimeFormatter.ofPattern("MMM d HH:mm:ss", Locale.US),
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss", Locale.US)
    )

    fun parseInstantOrNull(value: String): Instant? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null

        parseEpoch(trimmed)?.let { return it }

        try {
            return Instant.parse(trimmed)
        } catch (_: DateTimeParseException) {
            // Fall through to common log formats.
        }

        return dateTimeFormatters.firstNotNullOfOrNull { formatter ->
            parseWithFormatter(trimmed, formatter)
        }
    }

    fun entryInstant(entry: LogEntry): Instant? {
        return entry.instant ?: parseInstantOrNull(entry.timestamp.value)
    }

    fun validationMessage(
        fromValue: String,
        fromInstant: Instant?,
        toValue: String,
        toInstant: Instant?
    ): String? {
        if (fromValue.isNotBlank() && fromInstant == null) {
            return "Could not parse 'From' date/time"
        }

        if (toValue.isNotBlank() && toInstant == null) {
            return "Could not parse 'To' date/time"
        }

        return if (fromInstant != null && toInstant != null && fromInstant.isAfter(toInstant)) {
            "From must be before or equal to To"
        } else {
            null
        }
    }

    fun resolveRange(window: LogWindow): Pair<Instant, Instant>? {
        return when (window.timeFilterPreset) {
            TimeRangePreset.LAST_5_MINUTES -> {
                val end = window.logs.asSequence().mapNotNull(::entryInstant).maxOrNull() ?: Instant.now()
                val start = end.minus(5, ChronoUnit.MINUTES)
                start to end
            }

            null -> {
                val from = window.timeFilterFromInstant
                val to = window.timeFilterToInstant

                if (from == null && to == null) {
                    null
                } else {
                    val rangeStart = from ?: Instant.MIN
                    val rangeEnd = to ?: Instant.MAX
                    if (rangeStart.isAfter(rangeEnd)) null else rangeStart to rangeEnd
                }
            }
        }
    }

    fun toPreset(minutes: Long?): TimeRangePreset? {
        return when (minutes) {
            5L -> TimeRangePreset.LAST_5_MINUTES
            else -> null
        }
    }

    fun toMinutes(preset: TimeRangePreset?): Long? {
        return when (preset) {
            TimeRangePreset.LAST_5_MINUTES -> 5L
            null -> null
        }
    }

    private fun parseEpoch(value: String): Instant? {
        if (!value.all(Char::isDigit)) return null

        return when (value.length) {
            10 -> Instant.ofEpochSecond(value.toLong())
            13 -> Instant.ofEpochMilli(value.toLong())
            else -> null
        }
    }

    private fun parseWithFormatter(value: String, formatter: DateTimeFormatter): Instant? {
        return try {
            val accessor = formatter.parse(value)
            toInstant(accessor)
        } catch (_: DateTimeParseException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun toInstant(accessor: TemporalAccessor): Instant {
        if (accessor.isSupported(ChronoField.INSTANT_SECONDS)) {
            return Instant.from(accessor)
        }

        if (accessor.isSupported(ChronoField.OFFSET_SECONDS)) {
            return OffsetDateTime.from(accessor).toInstant()
        }

        val nowUtc = LocalDateTime.now(ZoneOffset.UTC)
        val year = temporalFieldOrDefault(accessor, ChronoField.YEAR, nowUtc.year)
        val month = temporalFieldOrDefault(accessor, ChronoField.MONTH_OF_YEAR, nowUtc.monthValue)
        val day = temporalFieldOrDefault(accessor, ChronoField.DAY_OF_MONTH, nowUtc.dayOfMonth)
        val hour = temporalFieldOrDefault(accessor, ChronoField.HOUR_OF_DAY, 0)
        val minute = temporalFieldOrDefault(accessor, ChronoField.MINUTE_OF_HOUR, 0)
        val second = temporalFieldOrDefault(accessor, ChronoField.SECOND_OF_MINUTE, 0)
        val nano = temporalFieldOrDefault(accessor, ChronoField.NANO_OF_SECOND, 0)

        return LocalDateTime.of(year, month, day, hour, minute, second, nano).toInstant(ZoneOffset.UTC)
    }

    private fun temporalFieldOrDefault(accessor: TemporalAccessor, field: ChronoField, default: Int): Int {
        return if (accessor.isSupported(field)) accessor.get(field) else default
    }
}