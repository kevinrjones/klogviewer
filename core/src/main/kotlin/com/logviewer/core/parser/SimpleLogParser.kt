package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.*
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

        val levelCandidate1 = levelMapper.map(levelRaw)
        val levelCandidate2 = if (levelCandidate1 == LogLevel.UNKNOWN) levelMapper.map(metadataRaw) else LogLevel.UNKNOWN

        val (level, content) = when {
            levelCandidate1 != LogLevel.UNKNOWN -> levelCandidate1 to contentRaw
            levelCandidate2 != LogLevel.UNKNOWN -> levelCandidate2 to "$levelRaw $contentRaw"
            else -> LogLevel.UNKNOWN to "${metadataRaw?.let { "[$it] " } ?: ""}$levelRaw $contentRaw"
        }

        val fields = mutableMapOf(
            "timestamp" to timestamp,
            "level" to level.name,
            "content" to content.trim()
        )
        metadataRaw?.let { fields["metadata"] = it }

        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = level,
            content = LogContent(content.trim()),
            fields = fields,
            instant = timestampParser.parse(timestamp)
        ).right()
    }
}
