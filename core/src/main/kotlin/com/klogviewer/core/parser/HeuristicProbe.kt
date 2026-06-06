package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

data class ProbeResult(
    val parser: LogParser,
    val parserName: String,
    val columns: List<String> = emptyList(),
    val confidence: ParseDetectionConfidence? = null
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

class HeuristicProbe(val registry: ParserRegistry) {
    
    /**
     * Attempts to detect the best parser for the given sample lines.
     * Defaults to [SimpleLogParser] if no better match is found.
     */
    fun detect(lines: List<String>): ProbeResult {
        if (lines.isEmpty()) return ProbeResult(SimpleLogParser(), "Simple")

        // 1. JSON Detection using structured confidence
        val jsonAnalysis = analyzeJson(lines)
        if (jsonAnalysis.shouldSelectJson) {
            logger.info { "Heuristic: Detected JSON log format" }
            val mapping = detectJsonMapping(jsonAnalysis.parsedJsonObjects)
            val columns = deriveJsonColumns(mapping, jsonAnalysis.parsedJsonObjects.firstOrNull())
            return ProbeResult(
                parser = JsonLogParser(mapping),
                parserName = "JSON",
                columns = columns,
                confidence = jsonAnalysis.confidence
            )
        }

        // 2. Template Matching (Prefer specific templates over generic logfmt)
        val templates = registry.getAllTemplates()
        val matchCounts = templates.associateWith { template ->
            val regex = template.regex.toRegex()
            lines.count { line -> regex.matches(line.trim()) }
        }

        val bestMatch = matchCounts.maxByOrNull { it.value }
        if (bestMatch != null && bestMatch.value > lines.size / 2) {
            logger.info { "Heuristic: Detected template [${bestMatch.key.name}] with ${bestMatch.value}/${lines.size} matches" }
            return ProbeResult(
                parser = TemplateLogParser(bestMatch.key),
                parserName = bestMatch.key.name,
                columns = bestMatch.key.columns,
                confidence = jsonAnalysis.confidence
            )
        }

        // 3. logfmt Detection
        val logfmtCount = lines.count { isLogfmt(it) }
        if (logfmtCount > lines.size / 2) {
            logger.info { "Heuristic: Detected logfmt log format" }
            return ProbeResult(LogfmtParser(), "logfmt", confidence = jsonAnalysis.confidence)
        }
        
        // 4. Fallback to best template match if any, or SimpleLogParser
        return if (bestMatch != null && bestMatch.value > 0) {
            logger.info { "Heuristic: Falling back to template [${bestMatch.key.name}] with ${bestMatch.value}/${lines.size} matches" }
            ProbeResult(
                parser = TemplateLogParser(bestMatch.key),
                parserName = bestMatch.key.name,
                columns = bestMatch.key.columns,
                confidence = jsonAnalysis.confidence
            )
        } else {
            logger.info { "Heuristic: No match found, falling back to SimpleLogParser" }
            ProbeResult(SimpleLogParser(), "Simple", confidence = jsonAnalysis.confidence)
        }
    }

    private fun deriveJsonColumns(mapping: JsonMapping, firstJson: JsonObject?): List<String> {
        if (firstJson == null) return listOf("Timestamp", "Level", "Content")

        val keys = firstJson.keys.toMutableSet()
        val resultColumns = mutableListOf<String>()

        if (keys.remove(mapping.timestampKey)) resultColumns.add("Timestamp")
        if (keys.remove(mapping.levelKey)) resultColumns.add("Level")
        if (keys.remove(mapping.contentKey)) resultColumns.add("Content")

        resultColumns.addAll(keys.sorted().map { it.replaceFirstChar { c -> c.uppercase() } })
        return resultColumns
    }

    private fun detectJsonMapping(parsedObjects: List<JsonObject>): JsonMapping {
        val keys = parsedObjects
            .flatMap { it.keys }
            .toSet()
            .ifEmpty { return JsonMapping() }

        val timestampKey = keys.firstAvailableKey(TIMESTAMP_KEYS)
        val levelKey = keys.firstAvailableKey(LEVEL_KEYS)
        val contentKey = keys.firstAvailableKey(CONTENT_KEYS)

        return JsonMapping(timestampKey, levelKey, contentKey)
    }

    private fun Set<String>.firstAvailableKey(candidates: List<String>): String =
        candidates.firstOrNull { it in this } ?: candidates.first()

    companion object {
        private val TIMESTAMP_KEYS = listOf("timestamp", "@timestamp", "time", "ts", "@t", "Timestamp")
        private val LEVEL_KEYS = listOf("level", "severity", "lvl", "@l", "LogLevel", "Level")
        private val CONTENT_KEYS = listOf("message", "msg", "content", "body", "@m", "@mt", "Message")
        private val LOGGER_KEYS = listOf("logger", "logger_name", "SourceContext", "Category", "CategoryName")
        private val EXCEPTION_KEYS = listOf("exception", "error", "stackTrace", "Exception", "@x")
        private val TRACE_ID_KEYS = listOf("traceId", "TraceId", "@tr")
        private val SPAN_ID_KEYS = listOf("spanId", "SpanId", "@sp")

        private const val JSON_CONFIDENCE_THRESHOLD = 0.45
        private const val LOW_SAMPLE_SINGLE_RECORD_PENALTY = 0.25
        private const val LOW_SAMPLE_DOUBLE_RECORD_NO_CANONICAL_PENALTY = 0.15
        private const val LOW_SAMPLE_DOUBLE_RECORD_CANONICAL_PENALTY = 0.05
    }

    private fun isLogfmt(line: String): Boolean {
        val trimmed = line.trim()
        // Logfmt should have at least 2 pairs or start with a pair to avoid false positives with standard logs
        val regex = """\w+=(?:"[^"]*"|\S+)""".toRegex()
        val matches = regex.findAll(trimmed).toList()
        return matches.size >= 2 || (matches.size == 1 && trimmed.startsWith(matches[0].value))
    }

    private fun analyzeJson(lines: List<String>): JsonDetectionAnalysis {
        val sampleStats = collectJsonSamples(lines)
        val confidence = buildJsonConfidence(
            parsedObjects = sampleStats.parsedObjects,
            sampledCount = lines.size,
            malformedCount = sampleStats.malformedCount
        )

        return JsonDetectionAnalysis(
            parsedJsonObjects = sampleStats.parsedObjects,
            confidence = confidence,
            shouldSelectJson =
                confidence.successfulParseCount > 0 &&
                    confidence.finalConfidenceScore >= JSON_CONFIDENCE_THRESHOLD
        )
    }

    private fun collectJsonSamples(lines: List<String>): JsonSampleStats {
        val parsedObjects = mutableListOf<JsonObject>()
        var malformedCount = 0

        lines.forEach { line ->
            val trimmed = line.trim()
            if (!looksJsonLike(trimmed)) {
                return@forEach
            }

            val parsed = runCatching { Json.parseToJsonElement(trimmed) }.getOrNull()
            if (parsed == null) {
                malformedCount += 1
                return@forEach
            }

            if (parsed is JsonObject) {
                parsedObjects.add(parsed)
            }
        }

        return JsonSampleStats(parsedObjects = parsedObjects, malformedCount = malformedCount)
    }

    private fun buildJsonConfidence(
        parsedObjects: List<JsonObject>,
        sampledCount: Int,
        malformedCount: Int
    ): ParseDetectionConfidence {
        val successfulParseCount = parsedObjects.size
        val parseSuccessRatio = successfulParseCount.toRatio(sampledCount)
        val malformedRatio = malformedCount.toRatio(sampledCount)
        val canonicalKeyHitCount = parsedObjects.sumOf { jsonObject ->
            canonicalKeyGroups.count { group -> jsonObject.keys.any { key -> key in group } }
        }
        val maxCanonicalHits = max(1, successfulParseCount * canonicalKeyGroups.size)
        val canonicalKeyHitRatio = canonicalKeyHitCount.toRatio(maxCanonicalHits)
        val lowSamplePenalty = when {
            sampledCount == 1 && canonicalKeyHitCount == 0 -> LOW_SAMPLE_SINGLE_RECORD_PENALTY
            sampledCount <= 2 && canonicalKeyHitCount == 0 -> LOW_SAMPLE_DOUBLE_RECORD_NO_CANONICAL_PENALTY
            sampledCount <= 2 -> LOW_SAMPLE_DOUBLE_RECORD_CANONICAL_PENALTY
            else -> 0.0
        }

        val finalScore = (
            (parseSuccessRatio * 0.65) +
                (canonicalKeyHitRatio * 0.35) -
                (malformedRatio * 0.45) -
                lowSamplePenalty
            ).coerceToUnitRange()

        return ParseDetectionConfidence(
            parserName = "JSON",
            sampledRecordCount = sampledCount,
            successfulParseCount = successfulParseCount,
            malformedCount = malformedCount,
            parseSuccessRatio = parseSuccessRatio,
            malformedRatio = malformedRatio,
            canonicalKeyHitCount = canonicalKeyHitCount,
            canonicalKeyHitRatio = canonicalKeyHitRatio,
            finalConfidenceScore = finalScore,
            debugFactors = mapOf(
                "parseSuccessRatio" to parseSuccessRatio,
                "canonicalKeyHitRatio" to canonicalKeyHitRatio,
                "malformedRatio" to malformedRatio,
                "lowSamplePenalty" to lowSamplePenalty
            )
        )
    }

    private fun looksJsonLike(trimmedLine: String): Boolean {
        val startsLikeJson = trimmedLine.startsWith("{") || trimmedLine.startsWith("[")
        val endsLikeJson = trimmedLine.endsWith("}") || trimmedLine.endsWith("]")
        return startsLikeJson || endsLikeJson
    }

    private fun Int.toRatio(total: Int): Double {
        if (total <= 0) return 0.0
        return toDouble() / total.toDouble()
    }

    private fun Double.coerceToUnitRange(): Double = min(1.0, max(0.0, this))

    private val canonicalKeyGroups: List<Set<String>> = listOf(
        TIMESTAMP_KEYS.toSet(),
        LEVEL_KEYS.toSet(),
        CONTENT_KEYS.toSet(),
        LOGGER_KEYS.toSet(),
        EXCEPTION_KEYS.toSet(),
        TRACE_ID_KEYS.toSet(),
        SPAN_ID_KEYS.toSet()
    )

    private data class JsonDetectionAnalysis(
        val parsedJsonObjects: List<JsonObject>,
        val confidence: ParseDetectionConfidence,
        val shouldSelectJson: Boolean
    )

    private data class JsonSampleStats(
        val parsedObjects: List<JsonObject>,
        val malformedCount: Int
    )
}
