package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class TemplateLogParser(val template: LogTemplate) : LogParser {
    private val regex = template.regex.toRegex()
    private val timestampParser = TimestampParser(template.timestampPattern)
    private val groupNames = """\(\?\<(\w+)\>""".toRegex().findAll(template.regex).map { it.groupValues[1] }.toList()

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        val trimmedLine = line.trim()
        val matchResult = regex.matchEntire(trimmedLine) ?: run {
            logger.debug { "Template [${template.name}] failed to parse line: $line" }
            return LogFailure.ParsingError("Could not parse log line with template ${template.name}", line).left()
        }

        val groups = matchResult.groups as MatchNamedGroupCollection
        
        val fields = groupNames.associateWith { name ->
            try { groups[name]?.value ?: "" } catch (e: Exception) { "" }
        }

        val timestampRaw = fields["timestamp"] ?: ""
        val levelRaw = fields["level"]
        val metadataRaw = fields["metadata"]
        val contentRaw = fields["content"] ?: ""

        val levelCandidate1 = template.levelMapper.map(levelRaw)
        val levelCandidate2 = if (levelCandidate1 == LogLevel.UNKNOWN) template.levelMapper.map(metadataRaw) else LogLevel.UNKNOWN

        val (level, content) = when {
            levelCandidate1 != LogLevel.UNKNOWN -> levelCandidate1 to contentRaw
            levelCandidate2 != LogLevel.UNKNOWN -> levelCandidate2 to "${levelRaw?.let { if (it.isNotEmpty()) "$it " else "" } ?: ""}$contentRaw"
            else -> LogLevel.UNKNOWN to "${metadataRaw?.let { if (it.isNotEmpty()) "[$it] " else "" } ?: ""}${levelRaw?.let { if (it.isNotEmpty()) "$it " else "" } ?: ""}$contentRaw"
        }

        return LogEntry(
            timestamp = LogTimestamp(timestampRaw),
            level = level,
            content = LogContent(content.trim()),
            fields = fields,
            instant = timestampParser.parse(timestampRaw)
        ).right()
    }
}
