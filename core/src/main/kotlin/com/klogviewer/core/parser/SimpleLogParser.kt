package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class SimpleLogParser : LogParser {
    private val regex = """^\s*(?<timestamp>\d{4}[-/]\d{1,2}[-/]\d{1,2}[\sT]\s*\d{1,2}:\d{2}:\d{2}(?:\.\d+)?(?:\s*[+-]\d{2}:?\d{2}|Z)?)\s*(?:\[(?<metadata>.*?)\]\s*)?(?<level>\[.*?\]|\S+)?(?:\s+(?<content>.*))?$""".toRegex()
    private val levelMapper = LevelMapper()
    private val timestampParser = TimestampParser("yyyy-MM-dd HH:mm:ss[.SSS][ XXX]")

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        val trimmedLine = line.trim()
        val matchResult = regex.matchEntire(trimmedLine) ?: run {
            logger.debug { "Failed to parse line: $line" }
            return LogFailure.ParsingError("Could not parse log line", line).left()
        }
        
        val timestamp = matchResult.groups["timestamp"]?.value ?: ""
        val metadataRaw = matchResult.groups["metadata"]?.value
        val levelRaw = matchResult.groups["level"]?.value ?: ""
        val contentRaw = matchResult.groups["content"]?.value ?: ""

        // Smart level detection
        var finalLevel = LogLevel.UNKNOWN
        var finalContent = contentRaw
        var finalMetadata = metadataRaw ?: ""
        var finalLevelRaw: String? = null

        val level1 = levelMapper.map(levelRaw)
        val level2 = if (level1 == LogLevel.UNKNOWN) levelMapper.map(metadataRaw) else LogLevel.UNKNOWN

        when {
            level1 != LogLevel.UNKNOWN -> {
                finalLevel = level1
                finalContent = contentRaw
                finalLevelRaw = levelRaw
            }
            level2 != LogLevel.UNKNOWN -> {
                finalLevel = level2
                finalContent = if (levelRaw.isNotEmpty()) "$levelRaw $contentRaw" else contentRaw
                finalMetadata = "" // Since it was promoted to level
                finalLevelRaw = "[$metadataRaw]"
            }
            else -> {
                // Check if content starts with a level (look-ahead)
                val contentTrimmed = contentRaw.trim()
                if (contentTrimmed.isNotEmpty()) {
                    val parts = contentTrimmed.split(Regex("\\s+"), 2)
                    val candidate = levelMapper.map(parts[0])
                    if (candidate != LogLevel.UNKNOWN) {
                        finalLevel = candidate
                        finalContent = if (parts.size > 1) parts[1] else ""
                        finalLevelRaw = parts[0]
                        val cleanLevelRaw = levelRaw.removeSurrounding("[", "]")
                        finalMetadata = if (finalMetadata.isNotEmpty()) "$finalMetadata] [$cleanLevelRaw" else cleanLevelRaw
                    } else {
                        // No level found anywhere, merge everything into content to avoid "labeling"
                        finalLevel = LogLevel.UNKNOWN
                        val prefix = listOfNotNull(
                            metadataRaw?.let { "[$it]" },
                            levelRaw.takeIf { it.isNotEmpty() }
                        ).joinToString(" ")
                        finalContent = if (prefix.isNotEmpty()) "$prefix $contentRaw" else contentRaw
                        finalMetadata = ""
                    }
                } else {
                    finalLevel = LogLevel.UNKNOWN
                    val prefix = listOfNotNull(
                        metadataRaw?.let { "[$it]" },
                        levelRaw.takeIf { it.isNotEmpty() }
                    ).joinToString(" ")
                    finalContent = if (prefix.isNotEmpty()) "$prefix $contentRaw" else contentRaw
                    finalMetadata = ""
                }
            }
        }

        val fields = mutableMapOf(
            "timestamp" to timestamp,
            "level" to (finalLevelRaw ?: finalLevel.name),
            "content" to finalContent.trim()
        )
        if (finalMetadata.isNotEmpty()) fields["metadata"] = finalMetadata

        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = finalLevel,
            content = LogContent(finalContent.trim()),
            fields = fields,
            instant = timestampParser.parse(timestamp)
        ).right()
    }
}
