package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.SampleLineSpan

data class PatternDiagnosticResult(
    val errorOffset: Int,
    val message: String,
    val spans: List<SampleLineSpan>
)

internal class PatternDiagnosticTracer(
    private val compiler: PatternDraftCompiler
) {
    fun diagnose(draft: PatternDraft, line: String): PatternDiagnosticResult {
        val trimmedLine = line.trimEnd('\r', '\n')
        val content = trimmedLine.trim()
        val lineOffset = trimmedLine.indexOf(content).coerceAtLeast(0)

        if (content.isEmpty()) {
            return PatternDiagnosticResult(0, "Line is empty", emptyList())
        }

        val state = TraceState(content, lineOffset, line)
        val failure = draft.segments.firstNotNullOfOrNull { segment ->
            processSegment(segment, state)
        }

        return failure ?: checkTrailingContent(state)
    }

    private fun processSegment(segment: PatternSegment, state: TraceState): PatternDiagnosticResult? {
        if (state.cursor >= state.content.length) {
            return handleUnexpectedEol(segment, state)
        }
        return when (segment) {
            is PatternSegment.Delimiter -> matchDelimiter(segment, state)
            is PatternSegment.Token -> matchToken(segment, state)
        }
    }

    private fun handleUnexpectedEol(segment: PatternSegment, state: TraceState): PatternDiagnosticResult {
        val expected = when (segment) {
            is PatternSegment.Delimiter -> "delimiter '${segment.delimiter.value}'"
            is PatternSegment.Token -> "${segment.token.role.displayName} token"
        }
        val summary = state.matchedSummary()
        return PatternDiagnosticResult(
            errorOffset = state.currentOffset(),
            message = "$summary, but reached end of line: expected $expected",
            spans = state.spans
        )
    }

    private fun matchDelimiter(
        segment: PatternSegment.Delimiter,
        state: TraceState
    ): PatternDiagnosticResult? {
        val delimVal = segment.delimiter.value
        val delimRegex = ("^" + compiler.escapeDelimiter(delimVal)).toRegex()
        val match = delimRegex.find(state.remainingContent())
        return if (match != null && match.range.first == 0) {
            state.cursor += match.value.length
            null
        } else {
            val snippet = state.snippet()
            val summary = state.matchedSummary()
            PatternDiagnosticResult(
                errorOffset = state.currentOffset(),
                message = "$summary — expected delimiter '$delimVal' but found '$snippet'",
                spans = state.spans
            )
        }
    }

    private fun matchToken(
        segment: PatternSegment.Token,
        state: TraceState
    ): PatternDiagnosticResult? {
        val token = segment.token
        val tokenRegexStr = token.regexOverride ?: compiler.defaultTokenRegex(token)
        val tokenRegex = Regex("^($tokenRegexStr)")
        val match = tokenRegex.find(state.remainingContent())

        return when {
            match != null && match.value.isNotEmpty() -> {
                val matchLen = match.value.length
                val start = state.currentOffset()
                val end = start + matchLen - 1
                if (start in state.fullLine.indices && end in state.fullLine.indices) {
                    state.spans.add(SampleLineSpan(start..end, segment.id, token.role))
                }
                state.matchedFieldNames.add(token.role.displayName)
                state.cursor += matchLen
                null
            }
            compiler.isTokenOptional(token) -> {
                state.matchedFieldNames.add("${token.role.displayName} (omitted)")
                null
            }
            else -> {
                val snippet = state.snippet()
                val summary = state.matchedSummary()
                val roleName = token.role.displayName
                PatternDiagnosticResult(
                    errorOffset = state.currentOffset(),
                    message = "$summary — failed matching $roleName: expected $roleName pattern but found '$snippet'",
                    spans = state.spans
                )
            }
        }
    }

    private fun checkTrailingContent(state: TraceState): PatternDiagnosticResult {
        return if (state.cursor < state.content.length) {
            val snippet = state.snippet()
            val summary = state.matchedSummary()
            PatternDiagnosticResult(
                errorOffset = state.currentOffset(),
                message = "$summary, but unparsed trailing text remains: '$snippet'",
                spans = state.spans
            )
        } else {
            PatternDiagnosticResult(0, "Line does not match pattern structure", state.spans)
        }
    }

    private class TraceState(
        val content: String,
        val lineOffset: Int,
        val fullLine: String
    ) {
        var cursor: Int = 0
        val spans = mutableListOf<SampleLineSpan>()
        val matchedFieldNames = mutableListOf<String>()

        fun currentOffset(): Int = cursor + lineOffset

        fun remainingContent(): String = content.substring(cursor)

        fun snippet(maxLength: Int = 25): String {
            val rem = remainingContent()
            return if (rem.length > maxLength) rem.take(maxLength) + "..." else rem
        }

        fun matchedSummary(): String {
            return if (matchedFieldNames.isNotEmpty()) {
                "Matched ${matchedFieldNames.joinToString(", ")}"
            } else {
                "At start of line"
            }
        }
    }
}
