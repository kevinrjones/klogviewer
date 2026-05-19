package com.klogviewer.core.parser

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser

data class LogfmtMapping(
    val timestampKey: String = "time",
    val levelKey: String = "level",
    val contentKey: String = "msg"
)

class LogfmtParser(
    private val mapping: LogfmtMapping = LogfmtMapping(),
    private val timestampParser: TimestampParser = TimestampParser("yyyy-MM-dd HH:mm:ss")
) : LogParser {

    private val levelMapper = LevelMapper()

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        val pairs = parseLogfmt(line)
        if (pairs.isEmpty()) return LogFailure.ParsingError(line, "Not a logfmt line").left()

        val timestampRaw = pairs[mapping.timestampKey] ?: ""
        val levelRaw = pairs[mapping.levelKey]
        val content = pairs[mapping.contentKey] ?: line
        val mappedLevel = levelMapper.map(levelRaw)

        val fields = mutableMapOf(
            "timestamp" to timestampRaw,
            "level" to (levelRaw ?: mappedLevel.name),
            "content" to content
        )
        pairs.forEach { (key, value) ->
            if (key != mapping.timestampKey && key != mapping.levelKey && key != mapping.contentKey) {
                fields[key] = value
            }
        }

        return LogEntry(
            timestamp = LogTimestamp(timestampRaw),
            level = mappedLevel,
            content = LogContent(content),
            fields = fields,
            instant = timestampParser.parse(timestampRaw)
        ).right()
    }

    private fun parseLogfmt(line: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = """(\w+)=(?:"([^"]*)"|(\S+))""".toRegex()
        regex.findAll(line).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].takeIf { it.isNotEmpty() } ?: match.groupValues[3]
            result[key] = value
        }
        return result
    }
}
