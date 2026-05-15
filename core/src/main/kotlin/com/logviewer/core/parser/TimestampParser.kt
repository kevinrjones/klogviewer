package com.logviewer.core.parser

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.temporal.ChronoField
import java.util.Locale
import io.github.oshai.kotlinlogging.KotlinLogging

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
            } catch (e: Exception) {
                null
            }
        }

        // 2. Try DateTimeFormatter
        return try {
            val accessor = formatter.parse(input)
            
            val now = LocalDateTime.now(ZoneOffset.UTC)
            val year = if (accessor.isSupported(ChronoField.YEAR)) accessor.get(ChronoField.YEAR) else now.year
            val month = if (accessor.isSupported(ChronoField.MONTH_OF_YEAR)) accessor.get(ChronoField.MONTH_OF_YEAR) else now.monthValue
            val day = if (accessor.isSupported(ChronoField.DAY_OF_MONTH)) accessor.get(ChronoField.DAY_OF_MONTH) else now.dayOfMonth
            val hour = if (accessor.isSupported(ChronoField.HOUR_OF_DAY)) accessor.get(ChronoField.HOUR_OF_DAY) else 0
            val minute = if (accessor.isSupported(ChronoField.MINUTE_OF_HOUR)) accessor.get(ChronoField.MINUTE_OF_HOUR) else 0
            val second = if (accessor.isSupported(ChronoField.SECOND_OF_MINUTE)) accessor.get(ChronoField.SECOND_OF_MINUTE) else 0
            val nano = if (accessor.isSupported(ChronoField.NANO_OF_SECOND)) accessor.get(ChronoField.NANO_OF_SECOND) else 0
            
            LocalDateTime.of(year, month, day, hour, minute, second, nano).toInstant(ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.debug { "Failed to parse timestamp '$input' with pattern '$pattern': ${e.message}" }
            null
        }
    }
}
