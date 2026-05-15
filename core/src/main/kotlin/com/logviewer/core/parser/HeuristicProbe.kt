package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

data class ProbeResult(
    val parser: LogParser,
    val columns: List<String> = emptyList()
)

class HeuristicProbe(private val registry: ParserRegistry) {
    
    /**
     * Attempts to detect the best parser for the given sample lines.
     * Defaults to [SimpleLogParser] if no better match is found.
     */
    fun detect(lines: List<String>): ProbeResult {
        if (lines.isEmpty()) return ProbeResult(SimpleLogParser())

        // 1. JSON Detection
        val jsonCount = lines.count { isJson(it) }
        if (jsonCount > lines.size / 2) {
            logger.info { "Heuristic: Detected JSON log format" }
            return ProbeResult(JsonLogParser())
        }

        // 2. logfmt Detection
        val logfmtCount = lines.count { isLogfmt(it) }
        if (logfmtCount > lines.size / 2) {
            logger.info { "Heuristic: Detected logfmt log format" }
            return ProbeResult(LogfmtParser())
        }

        // 3. Template Matching
        val templates = registry.getAllTemplates()
        val matchCounts = templates.associateWith { template ->
            val regex = template.regex.toRegex()
            lines.count { regex.matches(it) }
        }

        val bestMatch = matchCounts.maxByOrNull { it.value }
        
        return if (bestMatch != null && bestMatch.value > 0) {
            logger.info { "Heuristic: Detected template [${bestMatch.key.name}] with ${bestMatch.value}/${lines.size} matches" }
            ProbeResult(TemplateLogParser(bestMatch.key), bestMatch.key.columns)
        } else {
            logger.info { "Heuristic: No match found, falling back to SimpleLogParser" }
            ProbeResult(SimpleLogParser())
        }
    }

    private fun isJson(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }

    private fun isLogfmt(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.contains("""\w+=(?:"[^"]*"|\S+)""".toRegex())
    }
}
