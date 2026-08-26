package com.klogviewer.core.parser

import arrow.core.Either
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class PatternDraftCompilerIntegrationTest {

    private val compiler = PatternDraftCompiler()

    @Test
    fun `should compile draft with duplicate roles using unique group names`() {
        val userId1 = PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = "userId")
        val userId2 = PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = "userId")
        val draft = PatternDraft(
            segments = listOf(
                PatternSegment.Token(userId1),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(userId2),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val compiled = compiler.compile(draft)
        expectThat(compiled.tokenMappings[0].groupName).isEqualTo("userId")
        expectThat(compiled.tokenMappings[1].groupName).isEqualTo("userId1")
    }

    @Test
    fun `should escape special delimiter characters and empty delimiters`() {
        val tsToken = PatternToken(
            role = PatternTokenRole.TIMESTAMP,
            formatPattern = "yyyy-MM-dd HH:mm:ss"
        )
        val draft = PatternDraft(
            segments = listOf(
                PatternSegment.Token(tsToken),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val compiled = compiler.compile(draft)
        val line = "2026-08-26 10:00:00 [main] INFO - Application running"

        val parser = TemplateLogParser(compiled.template)
        val parseResult = parser.parse(line)

        expectThat(parseResult is Either.Right).isTrue()
        val entry = (parseResult as Either.Right).value
        expectThat(entry.level).isEqualTo(LogLevel.INFO)
        expectThat(entry.fields["thread"]).isEqualTo("main")
        expectThat(entry.content.value).isEqualTo("Application running")
    }

    @Test
    fun `should make non-last message token non-greedy`() {
        val codeToken = PatternToken(
            role = PatternTokenRole.CUSTOM_PROPERTY,
            customPropertyName = "code"
        )
        val draft = PatternDraft(
            segments = listOf(
                PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd")),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE)),
                PatternSegment.Delimiter(PatternDelimiter(value = " END ")),
                PatternSegment.Token(codeToken)
            )
        )

        val compiled = compiler.compile(draft)
        val line = "2026-08-26 Some payload text END 200"

        val parser = TemplateLogParser(compiled.template)
        val parseResult = parser.parse(line)

        expectThat(parseResult is Either.Right).isTrue()
        val entry = (parseResult as Either.Right).value
        expectThat(entry.content.value).isEqualTo("Some payload text")
        expectThat(entry.fields["code"]).isEqualTo("200")
    }

    @Test
    fun `should match preview output with runtime parser output`() {
        val draft = PatternImporter.importPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %logger - %msg").draft
        val compiled = compiler.compile(draft)

        val sampleLines = listOf(
            "2026-08-26 10:15:30.123 [http-nio-8080-exec-1] INFO com.example.Controller - Request processed",
            "2026-08-26 10:15:31.456 [main] ERROR com.example.Service - Database failure"
        )

        val previewService = DefaultPatternPreviewService(compiler)
        val previewResult = previewService.computePreview(draft, sampleLines)

        expectThat(previewResult.matchedLineCount).isEqualTo(2)

        val parser = TemplateLogParser(compiled.template)
        sampleLines.forEachIndexed { index, line ->
            val runtimeResult = parser.parse(line)
            expectThat(runtimeResult is Either.Right).isTrue()
            val entry = (runtimeResult as Either.Right).value
            val previewRow = previewResult.previewRows[index]

            expectThat(entry.level.name).isEqualTo(previewRow.fields["Level"] ?: "")
            expectThat(entry.fields["thread"]).isEqualTo(previewRow.fields["Thread"] ?: "")
            expectThat(entry.content.value).isEqualTo(previewRow.fields["Message"] ?: "")
        }
    }
}
