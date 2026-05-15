package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LevelMapperTest {

    private val mapper = LevelMapper()

    @Test
    fun `should normalize standard levels`() {
        expectThat(mapper.map("DEBUG")).isEqualTo(LogLevel.DEBUG)
        expectThat(mapper.map("INFO")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("WARN")).isEqualTo(LogLevel.WARN)
        expectThat(mapper.map("ERROR")).isEqualTo(LogLevel.ERROR)
        expectThat(mapper.map("FATAL")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `should handle common abbreviations`() {
        expectThat(mapper.map("DBUG")).isEqualTo(LogLevel.DEBUG)
        expectThat(mapper.map("INF")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("WRN")).isEqualTo(LogLevel.WARN)
        expectThat(mapper.map("ERR")).isEqualTo(LogLevel.ERROR)
        expectThat(mapper.map("FTL")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `should handle alternative terminology`() {
        expectThat(mapper.map("TRACE")).isEqualTo(LogLevel.DEBUG)
        expectThat(mapper.map("SEVERE")).isEqualTo(LogLevel.ERROR)
        expectThat(mapper.map("CRITICAL")).isEqualTo(LogLevel.FATAL)
        expectThat(mapper.map("WARNING")).isEqualTo(LogLevel.WARN)
        expectThat(mapper.map("INFORMATION")).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `should handle case insensitivity and whitespace`() {
        expectThat(mapper.map(" debug ")).isEqualTo(LogLevel.DEBUG)
        expectThat(mapper.map("Info")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("warn")).isEqualTo(LogLevel.WARN)
    }

    @Test
    fun `should handle prefix-based matching`() {
        val prefixMapper = LevelMapper(usePrefixMatching = true)
        expectThat(prefixMapper.map("D")).isEqualTo(LogLevel.DEBUG)
        expectThat(prefixMapper.map("I")).isEqualTo(LogLevel.INFO)
        expectThat(prefixMapper.map("W")).isEqualTo(LogLevel.WARN)
        expectThat(prefixMapper.map("E")).isEqualTo(LogLevel.ERROR)
        expectThat(prefixMapper.map("F")).isEqualTo(LogLevel.FATAL)
    }

    @Test
    fun `should handle bracketed and parenthesized levels`() {
        expectThat(mapper.map("[INFO]")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("(INFO)")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("[INF]")).isEqualTo(LogLevel.INFO)
        expectThat(mapper.map("(WARN)")).isEqualTo(LogLevel.WARN)
    }

    @Test
    fun `should handle multiple brackets`() {
        // Current implementation only removes one pair
        expectThat(mapper.map("[[INFO]]")).isEqualTo(LogLevel.UNKNOWN)
    }
}
