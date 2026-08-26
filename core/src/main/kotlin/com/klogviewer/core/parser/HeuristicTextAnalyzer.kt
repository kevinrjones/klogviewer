package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole

private const val LOG_TIME_LENGTH_THRESHOLD = 15

internal class HeuristicTextAnalyzer {

    fun analyzeTextHeuristics(lines: List<String>): ProbeResult? {
        val sampleLine = lines.map { it.trim() }.firstOrNull { it.isNotBlank() } ?: return null
        val segments = mutableListOf<PatternSegment>()

        var remaining = parseTimestampSegment(sampleLine, segments)
        remaining = parseLevelSegment(remaining, segments)
        remaining = parseThreadSegment(remaining, segments)
        parseRemainingDelimiters(remaining, segments)

        val hasHeaderRole = segments.any { it is PatternSegment.Token && isPrimaryHeaderRole(it.token.role) }
        val result = if (hasHeaderRole) {
            if (segments.none { it is PatternSegment.Token && it.token.role == PatternTokenRole.MESSAGE }) {
                segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE)))
            }
            val draft = PatternDraft(name = "Detected Text Pattern", segments = segments)
            evaluateDraftOnLines(draft, lines)
        } else {
            null
        }
        return result
    }

    private fun isPrimaryHeaderRole(role: PatternTokenRole): Boolean {
        return role == PatternTokenRole.TIMESTAMP || role == PatternTokenRole.LEVEL
    }

    private fun parseTimestampSegment(sampleLine: String, segments: MutableList<PatternSegment>): String {
        val tsRegex = Regex(
            """^(\d{4}[-/]\d{1,2}[-/]\d{1,2}[\sT]\d{1,2}:\d{2}:\d{2}(?:\.\d+)?|""" +
                """\d{1,2}:\d{2}:\d{2}(?:\.\d+)?|[A-Z][a-z]{2}\s+\d+\s+\d{2}:\d{2}:\d{2})"""
        )
        val tsMatch = tsRegex.find(sampleLine) ?: return sampleLine

        val tsValue = tsMatch.value
        val formatPattern = when {
            tsValue.contains("T") -> "yyyy-MM-dd'T'HH:mm:ss.SSS"
            tsValue.contains("-") || tsValue.contains("/") -> "yyyy-MM-dd HH:mm:ss.SSS"
            tsValue.count { it == ':' } == 2 && tsValue.length < LOG_TIME_LENGTH_THRESHOLD -> "HH:mm:ss.SSS"
            else -> "MMM d HH:mm:ss"
        }
        val token = PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = formatPattern)
        segments.add(PatternSegment.Token(token))
        return sampleLine.substring(tsMatch.range.last + 1)
    }

    private fun parseLevelSegment(remaining: String, segments: MutableList<PatternSegment>): String {
        val levelRegex = Regex(
            """^(\s*)(?:\[(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\]|""" +
                """(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE))""",
            RegexOption.IGNORE_CASE
        )
        val levelMatch = levelRegex.find(remaining) ?: return remaining

        val prefixSpace = levelMatch.groupValues[1]
        if (prefixSpace.isNotEmpty()) {
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = prefixSpace)))
        } else if (segments.isNotEmpty()) {
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = " ")))
        }

        segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)))
        return remaining.substring(levelMatch.range.last + 1)
    }

    private fun parseThreadSegment(remaining: String, segments: MutableList<PatternSegment>): String {
        val threadRegex = Regex("""^(\s*)\[([^\]]+)\]""")
        val threadMatch = threadRegex.find(remaining) ?: return remaining

        val prefix = threadMatch.groupValues[1]
        segments.add(PatternSegment.Delimiter(PatternDelimiter(value = prefix.ifEmpty { " [" })))
        segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)))
        segments.add(PatternSegment.Delimiter(PatternDelimiter(value = "] ")))
        return remaining.substring(threadMatch.range.last + 1)
    }

    private fun parseRemainingDelimiters(remaining: String, segments: MutableList<PatternSegment>) {
        if (remaining.startsWith(" - ")) {
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = " - ")))
        } else if (remaining.startsWith(" ")) {
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = " ")))
        }
    }

    @Suppress("SwallowedException")
    private fun evaluateDraftOnLines(draft: PatternDraft, lines: List<String>): ProbeResult? {
        return try {
            val previewResult = DefaultPatternPreviewService().computePreview(draft, lines)
            if (previewResult.matchedLineCount > 0) {
                val compiled = PatternDraftCompiler().compile(draft)
                ProbeResult(
                    parser = TemplateLogParser(compiled.template),
                    parserName = compiled.template.name,
                    columns = compiled.template.columns,
                    draft = draft,
                    isJson = false,
                    matchedLineCount = previewResult.matchedLineCount,
                    totalSampleLineCount = lines.size,
                    diagnostics = listOf("Synthesized text draft matched ${previewResult.matchedLineCount} lines.")
                )
            } else {
                null
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
