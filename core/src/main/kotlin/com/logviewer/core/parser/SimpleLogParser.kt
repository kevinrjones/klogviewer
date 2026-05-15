package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class SimpleLogParser : LogParser {
    private val regex = """^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d{3})?(?:\s+[+-]\d{2}:?\d{2})?)\s+(?:\[(.*?)\]\s+)?(\S+)\s+(.*)$""".toRegex()
    private val levelMapper = LevelMapper()

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        val trimmedLine = line.trim()
        val matchResult = regex.matchEntire(trimmedLine) ?: run {
            logger.debug { "Failed to parse line: $line" }
            return LogFailure.ParsingError("Could not parse log line", line).left()
        }
        
        val timestamp = matchResult.groups[1]?.value ?: ""
        val g2 = matchResult.groups[2]?.value
        val g3 = matchResult.groups[3]?.value ?: ""
        val g4 = matchResult.groups[4]?.value ?: ""

        val levelG3 = levelMapper.map(g3)
        val levelG2 = if (levelG3 == LogLevel.UNKNOWN) levelMapper.map(g2) else LogLevel.UNKNOWN

        val (level, content) = when {
            levelG3 != LogLevel.UNKNOWN -> levelG3 to g4
            levelG2 != LogLevel.UNKNOWN -> levelG2 to "$g3 $g4"
            else -> LogLevel.UNKNOWN to "${g2?.let { "[$it] " } ?: ""}$g3 $g4"
        }

        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = level,
            content = LogContent(content.trim())
        ).right()
    }
}
