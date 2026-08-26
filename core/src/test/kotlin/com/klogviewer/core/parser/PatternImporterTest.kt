package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternTokenRole
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue

class PatternImporterTest {

    @Test
    fun `should import standard Logback pattern format string`() {
        val pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"
        val result = PatternImporter.importPattern(pattern)

        expectThat(result.draft.originalFormatSyntax).isEqualTo("logback")
        val tokens = result.draft.segments.filterIsInstance<PatternSegment.Token>()
        expectThat(tokens).hasSize(5)

        expectThat(tokens[0].token.role).isEqualTo(PatternTokenRole.TIMESTAMP)
        expectThat(tokens[0].token.formatPattern).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS")

        expectThat(tokens[1].token.role).isEqualTo(PatternTokenRole.THREAD)
        expectThat(tokens[2].token.role).isEqualTo(PatternTokenRole.LEVEL)
        expectThat(tokens[3].token.role).isEqualTo(PatternTokenRole.LOGGER)
        expectThat(tokens[4].token.role).isEqualTo(PatternTokenRole.MESSAGE)
    }

    @Test
    fun `should import Logback MDC custom property`() {
        val pattern = "%d %level [%X{userId}] %msg"
        val result = PatternImporter.importPattern(pattern)

        val tokens = result.draft.segments.filterIsInstance<PatternSegment.Token>()
        expectThat(tokens).hasSize(4)

        val mdcToken = tokens[2].token
        expectThat(mdcToken.role).isEqualTo(PatternTokenRole.CUSTOM_PROPERTY)
        expectThat(mdcToken.customPropertyName).isEqualTo("userId")
        expectThat(result.draft.placeholderAnnotations["userId"]).isEqualTo("{userId}")
    }

    @Test
    fun `should import Serilog text layout`() {
        val pattern = "{Timestamp:yyyy-MM-dd HH:mm:ss.SSS} [{Level}] [{ThreadId}] {SourceContext} - {Message} {OrderId}"
        val result = PatternImporter.importPattern(pattern)

        expectThat(result.draft.originalFormatSyntax).isEqualTo("serilog")
        val tokens = result.draft.segments.filterIsInstance<PatternSegment.Token>()
        expectThat(tokens).hasSize(6)

        expectThat(tokens[0].token.role).isEqualTo(PatternTokenRole.TIMESTAMP)
        expectThat(tokens[0].token.formatPattern).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS")

        expectThat(tokens[1].token.role).isEqualTo(PatternTokenRole.LEVEL)
        expectThat(tokens[2].token.role).isEqualTo(PatternTokenRole.THREAD)
        expectThat(tokens[3].token.role).isEqualTo(PatternTokenRole.LOGGER)
        expectThat(tokens[4].token.role).isEqualTo(PatternTokenRole.MESSAGE)

        val customToken = tokens[5].token
        expectThat(customToken.role).isEqualTo(PatternTokenRole.CUSTOM_PROPERTY)
        expectThat(customToken.customPropertyName).isEqualTo("OrderId")
        expectThat(result.draft.placeholderAnnotations["OrderId"]).isEqualTo("{OrderId}")
    }

    @Test
    fun `should import preset names`() {
        val logbackPreset = PatternImporter.importPattern("Logback / Log4J Standard")
        expectThat(logbackPreset.draft.name).isEqualTo("Logback / Log4J Standard")
        expectThat(logbackPreset.draft.segments.isNotEmpty()).isTrue()

        val serilogPreset = PatternImporter.importPattern("Serilog Text Layout")
        expectThat(serilogPreset.draft.name).isEqualTo("Serilog Text Layout")
        expectThat(serilogPreset.draft.segments.isNotEmpty()).isTrue()
    }

    @Test
    fun `should handle empty or malformed inputs gracefully`() {
        val emptyResult = PatternImporter.importPattern("")
        expectThat(emptyResult.draft.segments.isNotEmpty()).isTrue()
        expectThat(emptyResult.diagnostics.isNotEmpty()).isTrue()

        val unsupportedLogback = PatternImporter.importPattern("%d %level %unknownSpecifier %msg")
        expectThat(unsupportedLogback.diagnostics.isNotEmpty()).isTrue()
        val tokens = unsupportedLogback.draft.segments.filterIsInstance<PatternSegment.Token>()
        expectThat(tokens.any { it.token.role == PatternTokenRole.CUSTOM_PROPERTY }).isTrue()
    }

    @Test
    fun `should extract Serilog message template placeholders`() {
        val template = "Processing order {OrderId} for customer {CustomerId:u8}"
        val placeholders = PatternImporter.extractSerilogPlaceholders(template)

        expectThat(placeholders["OrderId"]).isEqualTo("{OrderId}")
        expectThat(placeholders["CustomerId"]).isEqualTo("{CustomerId:u8}")
    }
}
