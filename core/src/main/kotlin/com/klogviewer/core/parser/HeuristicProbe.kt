package com.klogviewer.core.parser

import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.parser.LogParser
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

private const val MIN_SAMPLE_MATCH_DENOMINATOR = 3

data class ProbeResult(
    val parser: LogParser,
    val parserName: String,
    val columns: List<String> = emptyList(),
    val confidence: ParseDetectionConfidence? = null,
    val draft: PatternDraft? = null,
    val isJson: Boolean = false,
    val matchedLineCount: Int = 0,
    val totalSampleLineCount: Int = 0,
    val diagnostics: List<String> = emptyList()
)

data class ParseDetectionConfidence(
    val parserName: String,
    val sampledRecordCount: Int,
    val successfulParseCount: Int,
    val malformedCount: Int,
    val parseSuccessRatio: Double,
    val malformedRatio: Double,
    val canonicalKeyHitCount: Int,
    val canonicalKeyHitRatio: Double,
    val finalConfidenceScore: Double,
    val debugFactors: Map<String, Double> = emptyMap()
)

class HeuristicProbe(
    val registry: ParserRegistry,
    private val jsonConfidenceScorer: JsonConfidenceScorer = JsonConfidenceScorer()
) {
    private val textAnalyzer = HeuristicTextAnalyzer()

    fun detect(lines: List<String>): ProbeResult {
        if (lines.isEmpty()) return handleEmptyLines()

        val jsonResult = detectJson(lines)
        if (jsonResult != null) return jsonResult

        val templateResult = detectTemplate(lines)
        if (templateResult != null) return templateResult

        val logfmtResult = detectLogfmt(lines)
        if (logfmtResult != null) return logfmtResult

        val dynamicDraftResult = textAnalyzer.analyzeTextHeuristics(lines)
        if (dynamicDraftResult != null &&
            dynamicDraftResult.matchedLineCount > lines.size / MIN_SAMPLE_MATCH_DENOMINATOR) {
            logger.info { "Heuristic: Synthesized text draft with ${dynamicDraftResult.matchedLineCount} matches" }
            return dynamicDraftResult
        }

        return fallbackResult(lines)
    }

    private fun handleEmptyLines(): ProbeResult {
        return ProbeResult(
            parser = SimpleLogParser(),
            parserName = "Simple",
            columns = listOf("Timestamp", "Level", "Content"),
            draft = PatternImporter.defaultFallbackDraft(),
            isJson = false,
            totalSampleLineCount = 0,
            diagnostics = listOf("No sample lines provided; defaulted to fallback draft.")
        )
    }

    private fun detectJson(lines: List<String>): ProbeResult? {
        val jsonAnalysis = analyzeJson(lines)
        if (!jsonAnalysis.shouldSelectJson) return null

        logger.info { "Heuristic: Detected JSON log format" }
        val mapping = detectJsonMapping(jsonAnalysis.parsedJsonObjects)
        val columns = deriveJsonColumns(mapping, jsonAnalysis.parsedJsonObjects.firstOrNull())
        return ProbeResult(
            parser = JsonLogParser(mapping),
            parserName = "JSON",
            columns = columns,
            confidence = jsonAnalysis.confidence,
            draft = null,
            isJson = true,
            matchedLineCount = jsonAnalysis.parsedJsonObjects.size,
            totalSampleLineCount = lines.size,
            diagnostics = listOf("Structured JSON format detected with authoritative JsonMapping.")
        )
    }

    private fun detectTemplate(lines: List<String>): ProbeResult? {
        val templates = registry.getAllTemplates()
        val matchCounts = templates.associateWith { template ->
            val regex = template.regex.toRegex()
            lines.count { line -> regex.matches(line.trim()) }
        }

        val bestMatch = matchCounts.maxByOrNull { it.value }
        if (bestMatch != null && bestMatch.value > lines.size / 2) {
            logger.info { "Heuristic: Detected template [${bestMatch.key.name}]" }
            return ProbeResult(
                parser = TemplateLogParser(bestMatch.key),
                parserName = bestMatch.key.name,
                columns = bestMatch.key.columns,
                draft = draftForTemplate(bestMatch.key),
                isJson = false,
                matchedLineCount = bestMatch.value,
                totalSampleLineCount = lines.size,
                diagnostics = listOf("Matched registered template '${bestMatch.key.name}'.")
            )
        }
        return null
    }

    private fun detectLogfmt(lines: List<String>): ProbeResult? {
        val logfmtCount = lines.count { isLogfmt(it) }
        if (logfmtCount > lines.size / 2) {
            logger.info { "Heuristic: Detected logfmt log format" }
            return ProbeResult(
                parser = LogfmtParser(),
                parserName = "logfmt",
                columns = listOf("Timestamp", "Level", "Content"),
                draft = PatternImporter.defaultFallbackDraft(),
                isJson = false,
                matchedLineCount = logfmtCount,
                totalSampleLineCount = lines.size,
                diagnostics = listOf("Key-value logfmt log format detected.")
            )
        }
        return null
    }

    private fun fallbackResult(lines: List<String>): ProbeResult {
        val jsonAnalysis = analyzeJson(lines)
        val bestMatch = registry.getAllTemplates().associateWith { template ->
            val regex = template.regex.toRegex()
            lines.count { line -> regex.matches(line.trim()) }
        }.maxByOrNull { it.value }

        return if (bestMatch != null && bestMatch.value > 0) {
            ProbeResult(
                parser = TemplateLogParser(bestMatch.key),
                parserName = bestMatch.key.name,
                columns = bestMatch.key.columns,
                confidence = jsonAnalysis.confidence,
                draft = draftForTemplate(bestMatch.key),
                isJson = false,
                matchedLineCount = bestMatch.value,
                totalSampleLineCount = lines.size,
                diagnostics = listOf("Low-confidence fallback to template '${bestMatch.key.name}'.")
            )
        } else {
            ProbeResult(
                parser = SimpleLogParser(),
                parserName = "Simple",
                columns = listOf("Timestamp", "Level", "Content"),
                confidence = jsonAnalysis.confidence,
                draft = PatternImporter.defaultFallbackDraft(),
                isJson = false,
                matchedLineCount = 0,
                totalSampleLineCount = lines.size,
                diagnostics = listOf("Unresolved text log layout; seeded fallback draft.")
            )
        }
    }

    private fun draftForTemplate(template: LogTemplate): PatternDraft {
        val presetString = when (template.name) {
            "Standard" -> "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %logger - %msg"
            "Syslog" -> "%d{MMM d HH:mm:ss} %X{hostname} %X{process} %msg"
            "ISO8601" -> "%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} %level %msg"
            "Apache" -> "%X{clientIp} - %X{user} [%d{dd/MMM/yyyy:HH:mm:ss Z}] \"%X{request}\" %X{status} %X{bytes}"
            "CSV" -> "%d{yyyy-MM-dd},%level,%msg"
            else -> null
        }

        if (presetString != null) {
            return PatternImporter.importPattern(presetString).draft
        }

        return draftFromColumns(template)
    }

    private fun draftFromColumns(template: LogTemplate): PatternDraft {
        return PatternDraft(
            name = template.name,
            segments = template.columns.flatMapIndexed { index, col ->
                val role = mapColumnToRole(col)
                val token = PatternToken(
                    role = role,
                    customPropertyName = if (role == PatternTokenRole.CUSTOM_PROPERTY) col else null,
                    formatPattern = if (role == PatternTokenRole.TIMESTAMP) template.timestampPattern else ""
                )
                val list = mutableListOf<PatternSegment>(PatternSegment.Token(token))
                if (index < template.columns.size - 1) {
                    list.add(PatternSegment.Delimiter(PatternDelimiter(value = " ")))
                }
                list
            }
        )
    }

    private fun mapColumnToRole(columnName: String): PatternTokenRole {
        return when (columnName.lowercase()) {
            "timestamp" -> PatternTokenRole.TIMESTAMP
            "level" -> PatternTokenRole.LEVEL
            "thread" -> PatternTokenRole.THREAD
            "logger" -> PatternTokenRole.LOGGER
            "content", "message" -> PatternTokenRole.MESSAGE
            "exception" -> PatternTokenRole.EXCEPTION
            else -> PatternTokenRole.CUSTOM_PROPERTY
        }
    }

    private fun analyzeJson(lines: List<String>): JsonDetectionAnalysis {
        val sampleStats = collectJsonSamples(lines)
        val confidence = jsonConfidenceScorer.score(
            parsedObjects = sampleStats.parsedObjects,
            sampledCount = lines.size,
            malformedCount = sampleStats.malformedCount
        )

        return JsonDetectionAnalysis(
            parsedJsonObjects = sampleStats.parsedObjects,
            confidence = confidence,
            shouldSelectJson = jsonConfidenceScorer.shouldSelectJson(confidence)
        )
    }
}
