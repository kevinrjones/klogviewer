package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

data class ProbeResult(
    val parser: LogParser,
    val parserName: String,
    val columns: List<String> = emptyList()
)

class HeuristicProbe(val registry: ParserRegistry) {
    
    /**
     * Attempts to detect the best parser for the given sample lines.
     * Defaults to [SimpleLogParser] if no better match is found.
     */
    fun detect(lines: List<String>): ProbeResult {
        if (lines.isEmpty()) return ProbeResult(SimpleLogParser(), "Simple")

        // 1. JSON Detection (High confidence if it matches)
        val jsonCount = lines.count { isJson(it) }
        if (jsonCount > lines.size / 2) {
            logger.info { "Heuristic: Detected JSON log format" }
            val mapping = detectJsonMapping(lines)
            val firstJson = lines.firstOrNull { isJson(it) }
            val columns = if (firstJson != null) {
                try {
                    val element = Json.parseToJsonElement(firstJson).jsonObject
                    val keys = element.keys.toMutableSet()
                    val resultColumns = mutableListOf<String>()
                    
                    if (keys.remove(mapping.timestampKey)) resultColumns.add("Timestamp")
                    if (keys.remove(mapping.levelKey)) resultColumns.add("Level")
                    if (keys.remove(mapping.contentKey)) resultColumns.add("Content")
                    
                    resultColumns.addAll(keys.sorted().map { it.replaceFirstChar { c -> c.uppercase() } })
                    resultColumns
                } catch (e: Exception) {
                    logger.debug(e) { "Failed to parse JSON log line: $firstJson" }
                    listOf("Timestamp", "Level", "Content")
                }
            } else listOf("Timestamp", "Level", "Content")
            
            return ProbeResult(JsonLogParser(mapping), "JSON", columns)
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
            return ProbeResult(TemplateLogParser(bestMatch.key), bestMatch.key.name, bestMatch.key.columns)
        }

        // 3. logfmt Detection
        val logfmtCount = lines.count { isLogfmt(it) }
        if (logfmtCount > lines.size / 2) {
            logger.info { "Heuristic: Detected logfmt log format" }
            return ProbeResult(LogfmtParser(), "logfmt")
        }
        
        // 4. Fallback to best template match if any, or SimpleLogParser
        return if (bestMatch != null && bestMatch.value > 0) {
            logger.info { "Heuristic: Falling back to template [${bestMatch.key.name}] with ${bestMatch.value}/${lines.size} matches" }
            ProbeResult(TemplateLogParser(bestMatch.key), bestMatch.key.name, bestMatch.key.columns)
        } else {
            logger.info { "Heuristic: No match found, falling back to SimpleLogParser" }
            ProbeResult(SimpleLogParser(), "Simple")
        }
    }

    private fun isJson(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }

    private fun detectJsonMapping(lines: List<String>): JsonMapping {
        val firstJson = lines.firstOrNull { isJson(it) } ?: return JsonMapping()
        return try {
            val element = Json.parseToJsonElement(firstJson).jsonObject

            val keys = element.keys

            val timestampKey = keys.firstAvailableKey(TIMESTAMP_KEYS)
            val levelKey = keys.firstAvailableKey(LEVEL_KEYS)
            val contentKey = keys.firstAvailableKey(CONTENT_KEYS)

            JsonMapping(timestampKey, levelKey, contentKey)
        } catch (e: Exception) {
            JsonMapping()
        }
    }

    private fun Set<String>.firstAvailableKey(candidates: List<String>): String =
        candidates.firstOrNull { it in this } ?: candidates.first()

    companion object {
        private val TIMESTAMP_KEYS = listOf("timestamp", "time", "ts")
        private val LEVEL_KEYS = listOf("level", "lvl", "severity")
        private val CONTENT_KEYS = listOf("message", "msg", "content", "body")
    }

    private fun isLogfmt(line: String): Boolean {
        val trimmed = line.trim()
        // Logfmt should have at least 2 pairs or start with a pair to avoid false positives with standard logs
        val regex = """\w+=(?:"[^"]*"|\S+)""".toRegex()
        val matches = regex.findAll(trimmed).toList()
        return matches.size >= 2 || (matches.size == 1 && trimmed.startsWith(matches[0].value))
    }
}
