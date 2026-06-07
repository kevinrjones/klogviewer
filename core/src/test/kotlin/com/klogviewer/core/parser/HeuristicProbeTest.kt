package com.klogviewer.core.parser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class HeuristicProbeTest {

    private val registry = ParserRegistry()
    private val probe = HeuristicProbe(registry)

    @Test
    fun `should detect JSON parser and columns`() {
        val lines = listOf(
            """{"timestamp": "2024-05-14", "message": "one", "user": "alice", "action": "login"}""",
            """{"timestamp": "2024-05-14", "message": "two", "user": "bob", "action": "logout"}"""
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<JsonLogParser>()
        expectThat(result.parserName).isEqualTo("JSON")
        expectThat(result.columns).isEqualTo(listOf("Timestamp", "Content", "Action", "User"))
        expectThat(result.confidence).isNotNull()
        expectThat(result.confidence?.sampledRecordCount).isEqualTo(2)
        expectThat(result.confidence?.successfulParseCount).isEqualTo(2)
        expectThat(result.confidence?.malformedCount).isEqualTo(0)
    }

    @Test
    fun `should detect custom JSON keys`() {
        val lines = listOf(
            """{"time": "2024-05-14", "msg": "one", "lvl": "INFO"}""",
            """{"time": "2024-05-14", "msg": "two", "lvl": "WARN"}"""
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<JsonLogParser>()
        expectThat(result.parserName).isEqualTo("JSON")
        expectThat(result.columns).isEqualTo(listOf("Timestamp", "Level", "Content"))
    }

    @Test
    fun `should auto select json parser for one line canonical json log`() {
        val lines = listOf(
            """{"@timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"single"}"""
        )

        val result = probe.detect(lines)

        expectThat(result.parser).isA<JsonLogParser>()
        expectThat(result.parserName).isEqualTo("JSON")
        expectThat(result.confidence).isNotNull()
        expectThat(result.confidence?.finalConfidenceScore ?: 0.0).isGreaterThan(0.45)
    }

    @Test
    fun `should keep selecting JSON when malformed fraction is small`() {
        val lines = listOf(
            """{"timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"one"}""",
            """{"timestamp":"2024-05-14T10:00:01Z","level":"WARN","message":"two"}""",
            """{"timestamp":"2024-05-14T10:00:02Z","level":"ERROR","message":"three"}""",
            "{ malformed json"
        )

        val result = probe.detect(lines)

        expectThat(result.parser).isA<JsonLogParser>()
        expectThat(result.parserName).isEqualTo("JSON")
        expectThat(result.confidence).isNotNull()
        expectThat(result.confidence?.successfulParseCount).isEqualTo(3)
        expectThat(result.confidence?.malformedCount).isEqualTo(1)
    }

    @Test
    fun `should not select JSON when content is mostly malformed or ambiguous`() {
        val lines = listOf(
            "{not valid",
            "not json at all",
            "just text",
            "[1,2,3"
        )

        val result = probe.detect(lines)

        expectThat(result.parser !is JsonLogParser).isTrue()
        expectThat(result.confidence).isNotNull()
        expectThat(result.confidence?.finalConfidenceScore ?: Double.MAX_VALUE).isLessThanOrEqualTo(0.45)
    }

    @Test
    fun `should increase json confidence when canonical keys are present`() {
        val canonicalLines = listOf(
            """{"@timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"canonical"}""",
            """{"@timestamp":"2024-05-14T10:00:01Z","level":"WARN","message":"canonical 2"}"""
        )
        val genericLines = listOf(
            """{"a":"2024-05-14T10:00:00Z","b":"INFO","c":"generic"}""",
            """{"a":"2024-05-14T10:00:01Z","b":"WARN","c":"generic 2"}"""
        )

        val canonicalResult = probe.detect(canonicalLines)
        val genericResult = probe.detect(genericLines)

        expectThat(canonicalResult.parser).isA<JsonLogParser>()
        expectThat(genericResult.parser).isA<JsonLogParser>()
        expectThat(canonicalResult.confidence).isNotNull()
        expectThat(genericResult.confidence).isNotNull()
        expectThat(canonicalResult.confidence?.canonicalKeyHitCount ?: 0)
            .isGreaterThan(genericResult.confidence?.canonicalKeyHitCount ?: 0)
        expectThat(canonicalResult.confidence?.finalConfidenceScore ?: 0.0)
            .isGreaterThan(genericResult.confidence?.finalConfidenceScore ?: 0.0)
    }

    @Test
    fun `should detect template parser`() {
        val lines = listOf(
            "May 14 15:24:08 host proc[123]: syslog message",
            "May 14 15:24:09 host proc[124]: another message"
        )
        val result = probe.detect(lines)
        expectThat(result.parser).isA<TemplateLogParser>()
        expectThat(result.parserName).isEqualTo("Syslog")
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
        expectThat(result.parserName).isEqualTo("Standard")
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
        expectThat(result.parserName).isEqualTo("Simple")
    }

    @Test
    fun `should keep low confidence fallback deterministic`() {
        val lines = listOf(
            "mostly noise",
            "neither template nor valid json"
        )

        val parserNames = (1..20).map {
            probe.detect(lines).parserName
        }.toSet()

        expectThat(parserNames).isEqualTo(setOf("Simple"))
    }
}
