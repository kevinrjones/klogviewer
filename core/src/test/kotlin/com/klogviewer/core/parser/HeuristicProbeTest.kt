package com.klogviewer.core.parser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class HeuristicProbeTest {

    private val registry = ParserRegistry()
    private val probe = HeuristicProbe(registry)

    @Test
    fun `should detect JSON parser`() {
        val lines = listOf(
            """{"timestamp": "2024-05-14", "message": "one"}""",
            """{"timestamp": "2024-05-14", "message": "two"}"""
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<JsonLogParser>()
    }

    @Test
    fun `should detect template parser`() {
        val lines = listOf(
            "May 14 15:24:08 host proc[123]: syslog message",
            "May 14 15:24:09 host proc[124]: another message"
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<TemplateLogParser>()
        expectThat(result.columns).isEqualTo(listOf("Timestamp", "Hostname", "Process", "Content"))
    }

    @Test
    fun `should detect Standard template for timezone-aware logs`() {
        val lines = listOf(
            "2026-05-08 00:27:56.321 +01:00 [INF] more stuff here",
            "2026-05-08 00:27:57.000 +01:00 [INF] second line"
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<TemplateLogParser>()
        val parser = result.parser as TemplateLogParser
        expectThat(parser.template.name).isEqualTo("Standard")
    }

    @Test
    fun `should fallback to SimpleLogParser`() {
        val lines = listOf(
            "completely unknown format",
            "no regex will match this"
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<SimpleLogParser>()
    }
}
