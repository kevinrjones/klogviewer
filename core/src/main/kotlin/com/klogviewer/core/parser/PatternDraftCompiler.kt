package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole

data class CompiledPatternToken(
    val segmentId: String,
    val token: PatternToken,
    val groupName: String,
    val columnName: String
)

data class CompiledPattern(
    val template: LogTemplate,
    val tokenMappings: List<CompiledPatternToken>,
    val regex: Regex
)

class PatternDraftCompiler {
    fun compile(draft: PatternDraft): CompiledPattern {
        val tokenMappings = mutableListOf<CompiledPatternToken>()
        val columns = mutableListOf<String>()
        val regexBuilder = StringBuilder("^\\s*")
        var timestampPattern = "yyyy-MM-dd HH:mm:ss[.SSS][ XXX]"
        var groupIndex = 0

        draft.segments.forEach { segment ->
            when (segment) {
                is PatternSegment.Delimiter -> {
                    regexBuilder.append(escapeDelimiter(segment.delimiter.value))
                }
                is PatternSegment.Token -> {
                    val token = segment.token
                    val groupName = generateGroupName(token, groupIndex++)
                    val colName = if (token.role == PatternTokenRole.CUSTOM_PROPERTY) {
                        token.customPropertyName?.takeIf { it.isNotBlank() } ?: "Custom"
                    } else {
                        token.role.displayName
                    }

                    tokenMappings.add(
                        CompiledPatternToken(
                            segmentId = segment.id,
                            token = token,
                            groupName = groupName,
                            columnName = colName
                        )
                    )
                    columns.add(colName)

                    if (token.role == PatternTokenRole.TIMESTAMP && token.formatPattern.isNotBlank()) {
                        timestampPattern = token.formatPattern
                    }

                    val tokenRegex = token.regexOverride ?: defaultTokenRegex(token)
                    if (isTokenOptional(token)) {
                        regexBuilder.append("(?:(?<$groupName>$tokenRegex))?")
                    } else {
                        regexBuilder.append("(?<$groupName>$tokenRegex)")
                    }
                }
            }
        }
        regexBuilder.append("$")

        val regexStr = regexBuilder.toString()
        val template = LogTemplate(
            name = draft.name,
            regex = regexStr,
            timestampPattern = timestampPattern,
            columns = columns
        )
        return CompiledPattern(
            template = template,
            tokenMappings = tokenMappings,
            regex = regexStr.toRegex()
        )
    }

    private fun isTokenOptional(token: PatternToken): Boolean {
        return token.isOptional || token.role in setOf(
            PatternTokenRole.LOGGER,
            PatternTokenRole.EXCEPTION,
            PatternTokenRole.CUSTOM_PROPERTY
        )
    }

    private fun generateGroupName(token: PatternToken, index: Int): String {
        return when (token.role) {
            PatternTokenRole.TIMESTAMP -> if (index == 0) "timestamp" else "timestamp_$index"
            PatternTokenRole.LEVEL -> "level"
            PatternTokenRole.THREAD -> "thread"
            PatternTokenRole.LOGGER -> "logger"
            PatternTokenRole.MESSAGE -> "content"
            PatternTokenRole.EXCEPTION -> "exception"
            PatternTokenRole.CUSTOM_PROPERTY -> {
                val sanitized = token.customPropertyName
                    ?.filter { it.isLetterOrDigit() }
                    ?.takeIf { it.isNotEmpty() }
                sanitized ?: "custom_$index"
            }
        }
    }

    private fun defaultTokenRegex(token: PatternToken): String {
        return when (token.role) {
            PatternTokenRole.TIMESTAMP -> {
                val fp = token.formatPattern
                when {
                    fp.contains("yyyy") || fp.contains("yyyy-MM-dd") ->
                        """\d{4}[-/]\d{1,2}[-/]\d{1,2}[\sT]\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?(?:\s*[+-]\d{2}:?\d{2}|Z)?"""
                    fp.contains("HH:mm:ss") ->
                        """\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?"""
                    fp.contains("MMM") ->
                        """[A-Za-z]{3}\s+\d{1,2}\s+\d{1,2}:\d{2}:\d{2}"""
                    else ->
                        """\d{4}[-/]\d{1,2}[-/]\d{1,2}[\sT]\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?(?:\s*[+-]\d{2}:?\d{2}|Z)?|\S+(?:\s+\S+)?"""
                }
            }
            PatternTokenRole.LEVEL -> """\[?[A-Za-z]+\]?"""
            PatternTokenRole.THREAD -> """[^\]\r\n]+"""
            PatternTokenRole.LOGGER -> """[^\s\-\]:|]+"""
            PatternTokenRole.MESSAGE -> """.*"""
            PatternTokenRole.EXCEPTION -> """.*"""
            PatternTokenRole.CUSTOM_PROPERTY -> """[^\s\]\r\n]+"""
        }
    }

    private fun escapeDelimiter(value: String): String {
        if (value.isBlank()) return """\s+"""
        val trimmed = value.trim()
        val escaped = Regex.escape(trimmed)
        val prefix = if (value.startsWith(" ")) """\s*""" else ""
        val suffix = if (value.endsWith(" ")) """\s*""" else ""
        return "$prefix$escaped$suffix"
    }
}
