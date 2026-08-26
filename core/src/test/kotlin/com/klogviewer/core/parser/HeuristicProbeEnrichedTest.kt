package com.klogviewer.core.parser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class HeuristicProbeEnrichedTest {

    private val registry = ParserRegistry()
    private val probe = HeuristicProbe(registry)

    @Test
    fun `should distinguish structured JSON detection from text pattern wizard draft`() {
        val jsonLines = listOf(
            """{"timestamp":"2026-08-26T10:00:00Z","level":"INFO","message":"Structured payload"}""",
            """{"timestamp":"2026-08-26T10:00:01Z","level":"WARN","message":"Second entry"}"""
        )

        val result = probe.detect(jsonLines)

        expectThat(result.isJson).isTrue()
        expectThat(result.parser).isA<JsonLogParser>()
        expectThat(result.draft).isNull()
    }

    @Test
    fun `should synthesize text draft for unrecognized standard text logs`() {
        val textLines = listOf(
            "2026-08-26 10:00:00.123 INFO [http-exec-1] User login succeeded",
            "2026-08-26 10:00:01.456 WARN [http-exec-2] High memory usage"
        )

        val result = probe.detect(textLines)

        expectThat(result.isJson).isFalse()
        expectThat(result.draft).isNotNull()
        expectThat(result.draft!!.hasTimestampToken).isTrue()
        expectThat(result.matchedLineCount).isEqualTo(2)
    }

    @Test
    fun `should provide fallback draft for completely unstructured text lines`() {
        val noiseLines = listOf(
            "random unformatted text line",
            "another non matching raw line"
        )

        val result = probe.detect(noiseLines)

        expectThat(result.isJson).isFalse()
        expectThat(result.parser).isA<SimpleLogParser>()
        expectThat(result.draft).isNotNull()
        expectThat(result.diagnostics.isNotEmpty()).isTrue()
        expectThat(result.matchedLineCount).isEqualTo(0)
    }
}
