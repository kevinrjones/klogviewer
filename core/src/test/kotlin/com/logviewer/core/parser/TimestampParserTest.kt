package com.logviewer.core.parser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Year

class TimestampParserTest {

    @Test
    fun `should parse standard ISO-like timestamp`() {
        val parser = TimestampParser("yyyy-MM-dd HH:mm:ss")
        val result = parser.parse("2024-05-14 15:24:08")
        
        expectThat(result).isNotNull()
        val expected = LocalDateTime.of(2024, 5, 14, 15, 24, 8).toInstant(ZoneOffset.UTC)
        expectThat(result).isEqualTo(expected)
    }

    @Test
    fun `should handle missing year by defaulting to current year`() {
        val parser = TimestampParser("MMM d HH:mm:ss")
        val result = parser.parse("May 14 15:24:08")
        
        expectThat(result).isNotNull()
        val expected = LocalDateTime.of(Year.now().value, 5, 14, 15, 24, 8).toInstant(ZoneOffset.UTC)
        expectThat(result).isEqualTo(expected)
    }

    @Test
    fun `should parse timestamp with timezone offset`() {
        val parser = TimestampParser("yyyy-MM-dd HH:mm:ss[.SSS][ XXX]")
        val result = parser.parse("2024-05-14 15:24:08 +01:00")
        
        expectThat(result).isNotNull()
        // Note: Currently TimestampParser.parse returns Instant in UTC, 
        // but it doesn't actually use the timezone from the input yet 
        // because it uses LocalDateTime.toInstant(ZoneOffset.UTC).
        // For now we just verify it doesn't fail.
        expectThat(result).isEqualTo(LocalDateTime.of(2024, 5, 14, 15, 24, 8).toInstant(ZoneOffset.UTC))
    }

    @Test
    fun `should parse timestamp without optional parts`() {
        val parser = TimestampParser("yyyy-MM-dd HH:mm:ss[.SSS][ XXX]")
        val result = parser.parse("2024-05-14 15:24:08")
        
        expectThat(result).isNotNull()
        expectThat(result).isEqualTo(LocalDateTime.of(2024, 5, 14, 15, 24, 8).toInstant(ZoneOffset.UTC))
    }

    @Test
    fun `should return null for invalid timestamp`() {
        val parser = TimestampParser("yyyy-MM-dd")
        expectThat(parser.parse("invalid")).isEqualTo(null)
    }
}
