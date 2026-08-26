package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.ui.mvi.LogWindow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant

class LogUpdateReducerTest {

    private fun entry(
        timestamp: String,
        message: String,
        sourceId: String,
        instant: Instant? = Instant.parse(timestamp)
    ): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = LogLevel.INFO,
            content = LogContent(message),
            sourceId = sourceId,
            instant = instant
        )
    }

    private fun multiSourceWindow(logs: List<LogEntry>): LogWindow {
        return LogWindow(
            id = "win1",
            filePath = "/logs/a.log, /logs/b.log",
            sourceIds = listOf("/logs/a.log", "/logs/b.log"),
            logs = logs
        )
    }

    @Test
    fun `given multi source window when out of order entry appended then it is inserted at timestamp position`() {
        val existing = listOf(
            entry("2026-08-25T10:00:00Z", "first", "/logs/a.log"),
            entry("2026-08-25T10:00:02Z", "third", "/logs/a.log")
        )
        val late = entry("2026-08-25T10:00:01Z", "second", "/logs/b.log")

        val reduced = LogUpdateReducer.reduce(
            multiSourceWindow(existing),
            LogUpdate.Appended(listOf(late)),
            "/logs/b.log"
        )

        expectThat(reduced.logs.map { it.content.value }).isEqualTo(listOf("first", "second", "third"))
    }

    @Test
    fun `given multi source window when in order entry appended then it stays at the tail`() {
        val existing = listOf(
            entry("2026-08-25T10:00:00Z", "first", "/logs/a.log"),
            entry("2026-08-25T10:00:01Z", "second", "/logs/b.log")
        )
        val next = entry("2026-08-25T10:00:02Z", "third", "/logs/a.log")

        val reduced = LogUpdateReducer.reduce(
            multiSourceWindow(existing),
            LogUpdate.Appended(listOf(next)),
            "/logs/a.log"
        )

        expectThat(reduced.logs.map { it.content.value }).isEqualTo(listOf("first", "second", "third"))
    }

    @Test
    fun `given entry without parseable timestamp when appended then it anchors to file order at the tail`() {
        val existing = listOf(
            entry("2026-08-25T10:00:00Z", "first", "/logs/a.log"),
            entry("2026-08-25T10:00:05Z", "second", "/logs/b.log")
        )
        val noTimestamp = entry("", "continuation", "/logs/a.log", instant = null)

        val reduced = LogUpdateReducer.reduce(
            multiSourceWindow(existing),
            LogUpdate.Appended(listOf(noTimestamp)),
            "/logs/a.log"
        )

        expectThat(reduced.logs.map { it.content.value }).isEqualTo(listOf("first", "second", "continuation"))
    }

    @Test
    fun `given single source window when entry appended then plain append behavior is preserved`() {
        val existing = listOf(
            entry("2026-08-25T10:00:02Z", "later", "/logs/a.log")
        )
        val earlier = entry("2026-08-25T10:00:00Z", "earlier", "/logs/a.log")
        val window = LogWindow(
            id = "win1",
            filePath = "/logs/a.log",
            sourceIds = listOf("/logs/a.log"),
            logs = existing
        )

        val reduced = LogUpdateReducer.reduce(window, LogUpdate.Appended(listOf(earlier)), "/logs/a.log")

        expectThat(reduced.logs.map { it.content.value }).isEqualTo(listOf("later", "earlier"))
    }

    @Test
    fun `given multiple out of order entries when appended then all are inserted correctly`() {
        val existing = listOf(
            entry("2026-08-25T10:00:00Z", "e0", "/logs/a.log"),
            entry("2026-08-25T10:00:04Z", "e4", "/logs/a.log")
        )
        val burst = listOf(
            entry("2026-08-25T10:00:03Z", "e3", "/logs/b.log"),
            entry("2026-08-25T10:00:01Z", "e1", "/logs/b.log"),
            entry("2026-08-25T10:00:02Z", "e2", "/logs/b.log")
        )

        val reduced = LogUpdateReducer.reduce(
            multiSourceWindow(existing),
            LogUpdate.Appended(burst),
            "/logs/b.log"
        )

        expectThat(reduced.logs.map { it.content.value }).isEqualTo(listOf("e0", "e1", "e2", "e3", "e4"))
    }
}
