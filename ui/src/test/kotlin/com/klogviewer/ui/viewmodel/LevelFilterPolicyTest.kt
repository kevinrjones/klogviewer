package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LevelFilterKey
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class LevelFilterPolicyTest {

    @Test
    fun `available levels are sorted with canonical order and stable fallback ordering`() {
        val logs = listOf(
            logEntry(level = LogLevel.INFO, rawLevel = "custom"),
            logEntry(level = LogLevel.INFO, rawLevel = "error"),
            logEntry(level = LogLevel.INFO, rawLevel = "warning"),
            logEntry(level = LogLevel.INFO, rawLevel = "warn"),
            logEntry(level = LogLevel.INFO, rawLevel = "debug"),
            logEntry(level = LogLevel.INFO, rawLevel = "trace"),
            logEntry(level = LogLevel.INFO, rawLevel = "unknown")
        )

        val available = LevelFilterPolicy.availableLevels(logs)

        expectThat(available.map { it.value }).containsExactly(
            "TRACE",
            "DEBUG",
            "WARN",
            "WARNING",
            "ERROR",
            "UNKNOWN",
            "CUSTOM"
        )
    }

    @Test
    fun `toggle removes existing filter and re-adds it when toggled again`() {
        val info = LevelFilterKey.fromLogLevel(LogLevel.INFO)
        val error = LevelFilterKey.fromLogLevel(LogLevel.ERROR)

        val afterRemoval = LevelFilterPolicy.toggle(setOf(info, error), error)
        val afterAdd = LevelFilterPolicy.toggle(afterRemoval, error)

        expectThat(afterRemoval).isEqualTo(setOf(info))
        expectThat(afterAdd).isEqualTo(setOf(info, error))
    }

    @Test
    fun `toggle all clears filters when all available levels are already enabled`() {
        val available = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.INFO),
            LevelFilterKey.fromLogLevel(LogLevel.ERROR)
        )

        val toggled = LevelFilterPolicy.toggleAll(filters = available, availableLevels = available)

        expectThat(toggled).isEqualTo(emptySet())
    }

    @Test
    fun `toggle all enables defaults when available levels are empty`() {
        val toggled = LevelFilterPolicy.toggleAll(filters = emptySet(), availableLevels = emptySet())

        expectThat(toggled).isEqualTo(LevelFilterPolicy.defaultFilters)
    }

    @Test
    fun `reconcile keeps all enabled behavior for updated available levels`() {
        val previousAvailable = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.INFO),
            LevelFilterKey.fromLogLevel(LogLevel.WARN)
        )
        val updatedAvailable = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.INFO),
            LevelFilterKey.fromLogLevel(LogLevel.WARN),
            LevelFilterKey.fromLogLevel(LogLevel.ERROR)
        )

        val reconciled = LevelFilterPolicy.reconcile(
            previousFilters = previousAvailable,
            previousAvailableLevels = previousAvailable,
            updatedAvailableLevels = updatedAvailable
        )

        expectThat(reconciled).isEqualTo(updatedAvailable)
    }

    @Test
    fun `reconcile intersects filters when partial selection is active`() {
        val previousAvailable = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.DEBUG),
            LevelFilterKey.fromLogLevel(LogLevel.INFO),
            LevelFilterKey.fromLogLevel(LogLevel.ERROR)
        )
        val previousFilters = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.DEBUG),
            LevelFilterKey.fromLogLevel(LogLevel.ERROR)
        )
        val updatedAvailable = setOf(
            LevelFilterKey.fromLogLevel(LogLevel.INFO),
            LevelFilterKey.fromLogLevel(LogLevel.ERROR)
        )

        val reconciled = LevelFilterPolicy.reconcile(
            previousFilters = previousFilters,
            previousAvailableLevels = previousAvailable,
            updatedAvailableLevels = updatedAvailable
        )

        expectThat(reconciled).isEqualTo(setOf(LevelFilterKey.fromLogLevel(LogLevel.ERROR)))
    }

    @Test
    fun `matches uses resolved raw level when present`() {
        val entry = logEntry(level = LogLevel.INFO, rawLevel = "error")
        val filters = setOf(LevelFilterKey.fromLogLevel(LogLevel.ERROR))

        expectThat(LevelFilterPolicy.matches(entry, filters)).isTrue()
        expectThat(LevelFilterPolicy.matches(entry, setOf(LevelFilterKey.fromLogLevel(LogLevel.INFO)))).isFalse()
    }

    private fun logEntry(level: LogLevel, rawLevel: String? = null): LogEntry {
        val fields = rawLevel?.let { mapOf(LogEntry.RAW_LEVEL_FIELD to it) }.orEmpty()
        return LogEntry(
            timestamp = LogTimestamp("2026-06-01T00:00:00Z"),
            level = level,
            content = LogContent("message"),
            fields = fields
        )
    }
}
