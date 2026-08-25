package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class PatternPreviewServiceTest {

    private val compiler = PatternDraftCompiler()
    private val previewService = DefaultPatternPreviewService(compiler)

    @Test
    fun `should compile pattern draft and produce matching preview rows and spans`() {
        val draft = PatternDraft(
            name = "Test Draft",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER)),
                PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLines = listOf(
            "2026-08-25 10:00:00.123 [main] INFO com.example.App - Server started",
            "2026-08-25 10:00:01.456 [worker-1] WARN com.example.Worker - High memory usage"
        )

        val result = previewService.computePreview(draft, sampleLines)

        expectThat(result.matchedLineCount).isEqualTo(2)
        expectThat(result.totalSampleLineCount).isEqualTo(2)
        expectThat(result.confidenceScore).isEqualTo(1.0f)
        expectThat(result.parseErrors).isEmpty()
        expectThat(result.previewRows.size).isEqualTo(2)

        val firstRow = result.previewRows[0]
        expectThat(firstRow.fields["Timestamp"]).isEqualTo("2026-08-25 10:00:00.123")
        expectThat(firstRow.fields["Thread"]).isEqualTo("main")
        expectThat(firstRow.fields["Level"]).isEqualTo("INFO")
        expectThat(firstRow.fields["Logger"]).isEqualTo("com.example.App")
        expectThat(firstRow.fields["Message"]).isEqualTo("Server started")

        val firstLineSpans = result.spansPerLine[0]
        expectThat(firstLineSpans.size).isEqualTo(5)
        expectThat(firstLineSpans.map { it.role }).containsExactly(
            PatternTokenRole.TIMESTAMP,
            PatternTokenRole.THREAD,
            PatternTokenRole.LEVEL,
            PatternTokenRole.LOGGER,
            PatternTokenRole.MESSAGE
        )
    }

    @Test
    fun `should detect errors when sample line does not match draft pattern`() {
        val draft = PatternDraft(
            name = "Test Draft",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLines = listOf(
            "2026-08-25 10:00:00.123 INFO Server started",
            "INVALID_NON_MATCHING_LINE"
        )

        val result = previewService.computePreview(draft, sampleLines)

        expectThat(result.matchedLineCount).isEqualTo(1)
        expectThat(result.totalSampleLineCount).isEqualTo(2)
        expectThat(result.confidenceScore).isEqualTo(0.5f)
        expectThat(result.parseErrors.size).isEqualTo(1)
        expectThat(result.parseErrors[0].lineIndex).isEqualTo(1)
        expectThat(result.parseErrors[0].errorOffset).isEqualTo(0)
        expectThat(result.parseErrors[0].message).contains("failed matching Timestamp")
    }

    @Test
    fun `should report partial match progress and exact failure offset on mismatched lines`() {
        val draft = PatternDraft(
            name = "Test Partial Match",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLine = "2026-08-25 10:00:00.123 MISMATCHED_DELIMITER ERROR - Test message"
        val result = previewService.computePreview(draft, listOf(sampleLine))

        expectThat(result.matchedLineCount).isEqualTo(0)
        expectThat(result.parseErrors.size).isEqualTo(1)
        val error = result.parseErrors[0]
        expectThat(error.errorOffset).isEqualTo(23)
        expectThat(error.message).contains("Matched Timestamp")
        expectThat(error.message).contains("expected delimiter ' ['")
    }

    @Test
    fun `should ensure preview results and TemplateLogParser results stay completely aligned`() {
        val draft = PatternDraft(
            name = "Aligned Test",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = "traceId")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val line = "2026-08-25 10:00:00 ERROR [trace-abc-123] Database query failed"

        // 1. Preview service result
        val previewResult = previewService.computePreview(draft, listOf(line))
        expectThat(previewResult.matchedLineCount).isEqualTo(1)
        val previewRow = previewResult.previewRows[0]

        // 2. Compiled template parsed by TemplateLogParser
        val compiled = compiler.compile(draft)
        val parser = TemplateLogParser(compiled.template)
        val parseResult = parser.parse(line)

        expectThat(parseResult.isRight()).isTrue()
        parseResult.map { entry ->
            expectThat(entry.timestamp.value).isEqualTo(previewRow.fields["Timestamp"])
            expectThat(entry.level).isEqualTo(LogLevel.ERROR)
            expectThat(entry.content.value).isEqualTo(previewRow.fields["Message"])
            expectThat(entry.fields["traceId"]).isEqualTo(previewRow.fields["traceId"])
        }
    }

    @Test
    fun `should parse lines with omitted logger and mixed sample lines without errors`() {
        val draft = PatternDraft(
            name = "Logback Standard",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER)),
                PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLines = listOf(
            "2026-08-23 07:22:02.776 [main] INFO - Logging initialized. Logs directory: /path/to/logs",
            "2026-08-23 07:22:03.100 [main] INFO com.klogviewer.core.Service - System ready",
            "2026-08-23 07:22:04.200 [worker] WARN - Cache missed"
        )

        val result = previewService.computePreview(draft, sampleLines)

        expectThat(result.matchedLineCount).isEqualTo(3)
        expectThat(result.totalSampleLineCount).isEqualTo(3)
        expectThat(result.confidenceScore).isEqualTo(1.0f)
        expectThat(result.parseErrors).isEmpty()

        // First line: omitted logger
        val row1 = result.previewRows[0]
        expectThat(row1.fields["Timestamp"]).isEqualTo("2026-08-23 07:22:02.776")
        expectThat(row1.fields["Thread"]).isEqualTo("main")
        expectThat(row1.fields["Level"]).isEqualTo("INFO")
        expectThat(row1.fields["Logger"]).isEqualTo("")
        expectThat(row1.fields["Message"]).isEqualTo("Logging initialized. Logs directory: /path/to/logs")

        // Second line: present logger
        val row2 = result.previewRows[1]
        expectThat(row2.fields["Timestamp"]).isEqualTo("2026-08-23 07:22:03.100")
        expectThat(row2.fields["Thread"]).isEqualTo("main")
        expectThat(row2.fields["Level"]).isEqualTo("INFO")
        expectThat(row2.fields["Logger"]).isEqualTo("com.klogviewer.core.Service")
        expectThat(row2.fields["Message"]).isEqualTo("System ready")

        // Third line: omitted logger
        val row3 = result.previewRows[2]
        expectThat(row3.fields["Level"]).isEqualTo("WARN")
        expectThat(row3.fields["Logger"]).isEqualTo("")
        expectThat(row3.fields["Message"]).isEqualTo("Cache missed")

        // Verify runtime TemplateLogParser alignment on line with omitted logger
        val compiled = compiler.compile(draft)
        val parser = TemplateLogParser(compiled.template)
        val parseResult = parser.parse(sampleLines[0])
        expectThat(parseResult.isRight()).isTrue()
        parseResult.map { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2026-08-23 07:22:02.776")
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Logging initialized. Logs directory: /path/to/logs")
            expectThat(entry.fields["logger"]).isEqualTo("")
        }
    }

    @Test
    fun `should parse timestamp with timezone offset and json message payload`() {
        val draft = PatternDraft(
            name = "Serilog Layout",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS +00:00")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLine = "2026-08-21 00:13:43.386 +00:00 [INF] Diagnostic data (1 of 2): " +
            "{\"AssemblyInfo\":{\"DotnetVersion\":\".NET 9.0.18\"}}"

        val result = previewService.computePreview(draft, listOf(sampleLine))

        expectThat(result.matchedLineCount).isEqualTo(1)
        expectThat(result.parseErrors).isEmpty()
        expectThat(result.previewRows[0].fields["Timestamp"]).isEqualTo("2026-08-21 00:13:43.386 +00:00")
        expectThat(result.previewRows[0].fields["Level"]).isEqualTo("INF")
        val expectedMessage = "Diagnostic data (1 of 2): {\"AssemblyInfo\":{\"DotnetVersion\":\".NET 9.0.18\"}}"
        expectThat(result.previewRows[0].fields["Message"]).isEqualTo(expectedMessage)
    }

    @Test
    fun `should parse uppercase Serilog pattern format strings like YYYY-MM-DD and ZZZZZ`() {
        val draft = PatternDraft(
            name = "Serilog Layout Uppercase",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "YYYY-MM-DD HH:mm:ss.SSS ZZZZZ")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )

        val sampleLine = "2026-08-21 00:13:43.386 +00:00 [INF] Diagnostic data (1 of 2): " +
            "{\"AssemblyInfo\":{\"DotnetVersion\":\".NET 9.0.18\"}}"

        val result = previewService.computePreview(draft, listOf(sampleLine))

        expectThat(result.matchedLineCount).isEqualTo(1)
        expectThat(result.parseErrors).isEmpty()
        expectThat(result.previewRows[0].fields["Timestamp"]).isEqualTo("2026-08-21 00:13:43.386 +00:00")
        expectThat(result.previewRows[0].fields["Level"]).isEqualTo("INF")
        expectThat(result.confidenceScore).isEqualTo(1.0f)
    }
}
