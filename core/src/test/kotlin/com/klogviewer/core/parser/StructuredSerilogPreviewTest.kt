package com.klogviewer.core.parser

import arrow.core.Either
import com.klogviewer.domain.model.PatternDraft
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class StructuredSerilogPreviewTest {

    @Test
    fun `should extract Serilog message template placeholders in preview without altering JSON runtime parser`() {
        val jsonSample = """{"@t":"2026-08-26T10:00:00Z","@l":"Information",""" +
            """"@mt":"User {UserId} completed order {OrderId}","UserId":123,"OrderId":"ORD-99"}"""
        val sampleLines = listOf(jsonSample)

        val previewService = DefaultPatternPreviewService()
        val draft = PatternDraft(name = "Structured Preview Draft")

        val previewResult = previewService.computePreview(draft, sampleLines)

        expectThat(previewResult.placeholderAnnotations["UserId"]).isEqualTo("{UserId}")
        expectThat(previewResult.placeholderAnnotations["OrderId"]).isEqualTo("{OrderId}")

        val jsonParser = JsonLogParser(JsonMapping(timestampKey = "@t", levelKey = "@l", contentKey = "@mt"))
        val parseResult = jsonParser.parse(jsonSample)

        expectThat(parseResult is Either.Right).isTrue()
        val entry = (parseResult as Either.Right).value
        expectThat(entry.content.value).isEqualTo("User {UserId} completed order {OrderId}")
        expectThat(entry.fields["UserId"]).isEqualTo("123")
        expectThat(entry.fields["OrderId"]).isEqualTo("ORD-99")
    }
}
