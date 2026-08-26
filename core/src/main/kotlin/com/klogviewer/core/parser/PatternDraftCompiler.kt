package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import java.util.regex.PatternSyntaxException

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
        require(draft.segments.any { it is PatternSegment.Token }) {
            "Cannot compile PatternDraft without any tokens."
        }

        val tokenMappings = mutableListOf<CompiledPatternToken>()
        val columns = mutableListOf<String>()
        val regexBuilder = StringBuilder("^\\s*")
        var timestampPattern = "yyyy-MM-dd HH:mm:ss[.SSS][ XXX]"
        val usedGroupNames = mutableSetOf<String>()

        draft.segments.forEach { segment ->
            when (segment) {
                is PatternSegment.Delimiter -> {
                    regexBuilder.append(escapeDelimiter(segment.delimiter.value))
                }
                is PatternSegment.Token -> {
                    val lastToken = draft.segments.filterIsInstance<PatternSegment.Token>().last()
                    val isLastToken = segment == lastToken
                    val ts = processTokenSegment(
                        segment, usedGroupNames, isLastToken, tokenMappings, columns, regexBuilder
                    )
                    if (ts != null) timestampPattern = ts
                }
            }
        }
        regexBuilder.append("$")

        val regexStr = regexBuilder.toString()
        val compiledRegex = try {
            regexStr.toRegex()
        } catch (e: PatternSyntaxException) {
            throw IllegalArgumentException("Failed to compile pattern regex: ${e.message}", e)
        }

        val template = LogTemplate(
            name = draft.name,
            regex = regexStr,
            timestampPattern = timestampPattern,
            columns = columns
        )

        return CompiledPattern(
            template = template,
            tokenMappings = tokenMappings,
            regex = compiledRegex
        )
    }

    private fun processTokenSegment(
        segment: PatternSegment.Token,
        usedGroupNames: MutableSet<String>,
        isLastToken: Boolean,
        tokenMappings: MutableList<CompiledPatternToken>,
        columns: MutableList<String>,
        regexBuilder: StringBuilder
    ): String? {
        val token = segment.token
        val groupName = generateUniqueGroupName(token, usedGroupNames)
        usedGroupNames.add(groupName)

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
        if (!columns.contains(colName)) {
            columns.add(colName)
        }

        val tokenRegex = token.regexOverride ?: defaultTokenRegex(token, isLastToken)
        if (isTokenOptional(token)) {
            regexBuilder.append("(?:(?<$groupName>$tokenRegex))?")
        } else {
            regexBuilder.append("(?<$groupName>$tokenRegex)")
        }

        return if (token.role == PatternTokenRole.TIMESTAMP && token.formatPattern.isNotBlank()) {
            token.formatPattern
        } else null
    }

    internal fun isTokenOptional(token: PatternToken): Boolean {
        return token.isOptional || token.role in setOf(
            PatternTokenRole.LOGGER,
            PatternTokenRole.EXCEPTION,
            PatternTokenRole.CUSTOM_PROPERTY
        )
    }

    private fun generateUniqueGroupName(token: PatternToken, usedNames: Set<String>): String {
        val baseName = when (token.role) {
            PatternTokenRole.TIMESTAMP -> "timestamp"
            PatternTokenRole.LEVEL -> "level"
            PatternTokenRole.THREAD -> "thread"
            PatternTokenRole.LOGGER -> "logger"
            PatternTokenRole.MESSAGE -> "content"
            PatternTokenRole.EXCEPTION -> "exception"
            PatternTokenRole.CUSTOM_PROPERTY -> {
                val sanitized = token.customPropertyName
                    ?.filter { it.isLetterOrDigit() }
                    ?.takeIf { it.isNotEmpty() && it.first().isLetter() }
                sanitized ?: "custom"
            }
        }

        if (baseName !in usedNames) return baseName

        var index = 1
        while ("${baseName}$index" in usedNames) {
            index++
        }
        return "${baseName}$index"
    }

    internal fun defaultTokenRegex(token: PatternToken, isLastToken: Boolean = true): String {
        return when (token.role) {
            PatternTokenRole.TIMESTAMP -> defaultTimestampRegex(token.formatPattern.lowercase())
            PatternTokenRole.LEVEL -> """\[?[A-Za-z]+\]?"""
            PatternTokenRole.THREAD -> """[^\]\r\n]+"""
            PatternTokenRole.LOGGER -> """[^\s\-\]:|]+"""
            PatternTokenRole.MESSAGE -> if (isLastToken) """.*""" else """.*?"""
            PatternTokenRole.EXCEPTION -> if (isLastToken) """.*""" else """.*?"""
            PatternTokenRole.CUSTOM_PROPERTY -> """[^\s\]\r\n]+"""
        }
    }

    private fun defaultTimestampRegex(fp: String): String {
        return when {
            fp.contains("yyyy") || fp.contains("yy") || fp.contains("iso8601") ->
                """\d{4}[-/]\d{1,2}[-/]\d{1,2}(?:[\sT]\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?(?:\s*[+-]\d{2}:?\d{2}|Z)?)?"""
            fp.contains("hh:mm:ss") ->
                """\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?"""
            fp.contains("mmm") ->
                """[A-Za-z]{3}\s+\d{1,2}\s+\d{1,2}:\d{2}:\d{2}"""
            else ->
                """\d{4}[-/]\d{1,2}[-/]\d{1,2}(?:[\sT]\d{1,2}:\d{2}:\d{2}(?:[.,]\d+)?(?:\s*[+-]\d{2}:?\d{2}|Z)?)?|\S+(?:\s+\S+)?"""
        }
    }

    internal fun escapeDelimiter(value: String): String {
        return when {
            value.isEmpty() -> ""
            value.isBlank() -> """\s+"""
            else -> {
                val trimmed = value.trim()
                val escaped = Regex.escape(trimmed)
                val prefix = if (value.startsWith(" ")) """\s*""" else ""
                val suffix = if (value.endsWith(" ")) """\s*""" else ""
                "$prefix$escaped$suffix"
            }
        }
    }
}
