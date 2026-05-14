package com.logviewer.core.parser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class SimpleLogParser : LogParser {
    private val regex = """^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) \[(\w+)\] (.*)$""".toRegex()

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        val trimmedLine = line.trim()
        val matchResult = regex.matchEntire(trimmedLine) ?: run {
            logger.debug { "Failed to parse line: $line" }
            return LogFailure.ParsingError("Could not parse log line", line).left()
        }
        
        val (timestamp, levelStr, content) = matchResult.destructured
        val level = when (levelStr.uppercase()) {
            "DEBUG" -> LogLevel.DEBUG
            "INFO" -> LogLevel.INFO
            "WARN" -> LogLevel.WARN
            "ERROR" -> LogLevel.ERROR
            "FATAL" -> LogLevel.FATAL
            else -> LogLevel.UNKNOWN
        }

        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = level,
            content = LogContent(content)
        ).right()
    }
}
