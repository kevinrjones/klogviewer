package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternParseError
import com.klogviewer.domain.model.PatternPreviewResult
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PreviewTableRow
import com.klogviewer.domain.model.SampleLineSpan

interface PatternPreviewService {
    fun computePreview(draft: PatternDraft, sampleLines: List<String>): PatternPreviewResult
}

class DefaultPatternPreviewService(
    private val compiler: PatternDraftCompiler = PatternDraftCompiler()
) : PatternPreviewService {
    private val tracer: PatternDiagnosticTracer = PatternDiagnosticTracer(compiler)

    @Suppress("TooGenericExceptionCaught")
    override fun computePreview(draft: PatternDraft, sampleLines: List<String>): PatternPreviewResult {
        return when {
            sampleLines.isEmpty() -> PatternPreviewResult()
            draft.segments.none { it is PatternSegment.Token } -> createEmptyTokenResult(sampleLines)
            else -> {
                try {
                    val compiled = compiler.compile(draft)
                    evaluateSampleLines(compiled, draft, sampleLines)
                } catch (e: Exception) {
                    createCompilationErrorResult(sampleLines, e.message ?: "Invalid pattern")
                }
            }
        }
    }

    private fun createEmptyTokenResult(sampleLines: List<String>): PatternPreviewResult {
        return PatternPreviewResult(
            spansPerLine = sampleLines.map { emptyList() },
            previewRows = sampleLines.mapIndexed { idx, _ -> PreviewTableRow(idx, emptyMap()) },
            columns = emptyList(),
            parseErrors = emptyList(),
            matchedLineCount = 0,
            totalSampleLineCount = sampleLines.size,
            confidenceScore = 0f
        )
    }

    private fun createCompilationErrorResult(sampleLines: List<String>, message: String): PatternPreviewResult {
        return PatternPreviewResult(
            spansPerLine = sampleLines.map { emptyList() },
            previewRows = sampleLines.mapIndexed { idx, _ -> PreviewTableRow(idx, emptyMap()) },
            columns = emptyList(),
            parseErrors = listOf(
                PatternParseError(
                    lineIndex = 0,
                    lineText = sampleLines.firstOrNull() ?: "",
                    errorOffset = 0,
                    message = "Invalid regex pattern: $message"
                )
            ),
            matchedLineCount = 0,
            totalSampleLineCount = sampleLines.size,
            confidenceScore = 0f
        )
    }

    private fun evaluateSampleLines(
        compiled: CompiledPattern,
        draft: PatternDraft,
        sampleLines: List<String>
    ): PatternPreviewResult {
        val spansPerLine = mutableListOf<List<SampleLineSpan>>()
        val previewRows = mutableListOf<PreviewTableRow>()
        val errors = mutableListOf<PatternParseError>()
        var matchedCount = 0

        sampleLines.forEachIndexed { lineIndex, line ->
            val parsed = parseSingleSampleLine(compiled, line)
            if (parsed.isMatched) {
                matchedCount++
                spansPerLine.add(parsed.spans)
                previewRows.add(PreviewTableRow(lineIndex, parsed.fields))
            } else {
                val diagnostic = tracer.diagnose(draft, line)
                spansPerLine.add(diagnostic.spans)
                previewRows.add(PreviewTableRow(lineIndex, emptyMap()))
                errors.add(
                    PatternParseError(
                        lineIndex = lineIndex,
                        lineText = line,
                        errorOffset = diagnostic.errorOffset,
                        message = diagnostic.message
                    )
                )
            }
        }

        val confidence = if (sampleLines.isNotEmpty()) {
            matchedCount.toFloat() / sampleLines.size.toFloat()
        } else {
            1.0f
        }

        return PatternPreviewResult(
            spansPerLine = spansPerLine,
            previewRows = previewRows,
            columns = compiled.template.columns,
            parseErrors = errors,
            matchedLineCount = matchedCount,
            totalSampleLineCount = sampleLines.size,
            confidenceScore = confidence
        )
    }

    private fun parseSingleSampleLine(
        compiled: CompiledPattern,
        line: String
    ): LineParseResult {
        val trimmedLine = line.trimEnd('\r', '\n')
        val trimmedContent = trimmedLine.trim()
        val matchResult = compiled.regex.matchEntire(trimmedContent)
            ?: return LineParseResult(false, emptyList(), emptyMap())

        val lineOffset = trimmedLine.indexOf(trimmedContent).coerceAtLeast(0)
        val lineSpans = mutableListOf<SampleLineSpan>()
        val rowFields = mutableMapOf<String, String>()

        compiled.tokenMappings.forEach { mapping ->
            val group = matchResult.groups[mapping.groupName]
            if (group != null && group.value.isNotEmpty()) {
                val start = group.range.first + lineOffset
                val end = group.range.last + lineOffset
                if (start in line.indices && end in line.indices) {
                    lineSpans.add(
                        SampleLineSpan(
                            range = start..end,
                            segmentId = mapping.segmentId,
                            role = mapping.token.role
                        )
                    )
                }
                rowFields[mapping.columnName] = group.value
            } else {
                rowFields[mapping.columnName] = ""
            }
        }
        return LineParseResult(true, lineSpans, rowFields)
    }

    private data class LineParseResult(
        val isMatched: Boolean,
        val spans: List<SampleLineSpan>,
        val fields: Map<String, String>
    )
}
