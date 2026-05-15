package com.klogviewer.core.parser

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAccessor
import java.util.*

private val logger = KotlinLogging.logger {}

class TimestampParser(private val pattern: String) {
    private val formatter = DateTimeFormatter.ofPattern(pattern).withLocale(Locale.US)

    fun parse(input: String): Instant? {
        if (input.isBlank()) return null
        
        // 1. Try Unix Epoch (Seconds or Millis)
        if (input.all { it.isDigit() }) {
            return try {
                val value = input.toLong()
                if (input.length <= 10) {
                    Instant.ofEpochSecond(value)
                } else {
                    Instant.ofEpochMilli(value)
                }
            } catch (_: Exception) {
                null
            }
        }

        // 2. Try DateTimeFormatter
        return try {
            val accessor = formatter.parse(input)
            
            if (accessor.isSupported(ChronoField.OFFSET_SECONDS)) {
                return Instant.from(accessor)
            }

            val now = LocalDateTime.now(ZoneOffset.UTC)
            val year = getField(accessor, ChronoField.YEAR, now.year)
            val month = getField(accessor, ChronoField.MONTH_OF_YEAR, now.monthValue)
            val day = getField(accessor, ChronoField.DAY_OF_MONTH, now.dayOfMonth)
            val hour = getField(accessor, ChronoField.HOUR_OF_DAY, 0)
            val minute = getField(accessor, ChronoField.MINUTE_OF_HOUR, 0)
            val second = getField(accessor, ChronoField.SECOND_OF_MINUTE, 0)
            val nano = getField(accessor, ChronoField.NANO_OF_SECOND, 0)
            
            LocalDateTime.of(year, month, day, hour, minute, second, nano).toInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.debug { "Failed to parse timestamp '$input' with pattern '$pattern': ${e.message}" }
            null
        }
    }

    private fun getField(accessor: TemporalAccessor, field: ChronoField, default: Int): Int {
        return if (accessor.isSupported(field)) accessor.get(field) else default
    }
}
