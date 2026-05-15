package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
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

        // Smart level detection
        var finalLevel = LogLevel.UNKNOWN
        var finalContent = contentRaw
        var finalMetadata = metadataRaw ?: ""
        var finalLevelRaw: String? = null

        val level1 = template.levelMapper.map(levelRaw)
        val level2 = if (level1 == LogLevel.UNKNOWN) template.levelMapper.map(metadataRaw) else LogLevel.UNKNOWN

        when {
            level1 != LogLevel.UNKNOWN -> {
                finalLevel = level1
                finalContent = contentRaw
                finalLevelRaw = levelRaw
            }
            level2 != LogLevel.UNKNOWN -> {
                finalLevel = level2
                finalContent = "${levelRaw?.let { if (it.isNotEmpty()) "$it " else "" } ?: ""}$contentRaw"
                finalMetadata = ""
                finalLevelRaw = "[$metadataRaw]"
            }
            else -> {
                // Check if content starts with a level (look-ahead)
                val contentTrimmed = contentRaw.trim()
                if (contentTrimmed.isNotEmpty()) {
                    val parts = contentTrimmed.split(Regex("\\s+"), 2)
                    val candidate = template.levelMapper.map(parts[0])
                    if (candidate != LogLevel.UNKNOWN) {
                        finalLevel = candidate
                        finalContent = if (parts.size > 1) parts[1] else ""
                        finalLevelRaw = parts[0]
                        val cleanLevelRaw = (levelRaw ?: "").removeSurrounding("[", "]")
                        finalMetadata = if (finalMetadata.isNotEmpty()) "$finalMetadata] [$cleanLevelRaw" else cleanLevelRaw
                    } else {
                        // No level found anywhere, merge everything into content to avoid "labeling"
                        finalLevel = LogLevel.UNKNOWN
                        val prefix = listOfNotNull(
                            metadataRaw?.let { "[$it]" },
                            levelRaw?.takeIf { it.isNotEmpty() }
                        ).joinToString(" ")
                        finalContent = if (prefix.isNotEmpty()) "$prefix $contentRaw" else contentRaw
                        finalMetadata = ""
                    }
                } else {
                    finalLevel = LogLevel.UNKNOWN
                    val prefix = listOfNotNull(
                        metadataRaw?.let { "[$it]" },
                        levelRaw?.takeIf { it.isNotEmpty() }
                    ).joinToString(" ")
                    finalContent = if (prefix.isNotEmpty()) "$prefix $contentRaw" else contentRaw
                    finalMetadata = ""
                }
            }
        }

        val updatedFields = fields.toMutableMap()
        updatedFields["level"] = finalLevelRaw ?: finalLevel.name
        if (finalMetadata.isNotEmpty()) updatedFields["metadata"] = finalMetadata
        else updatedFields.remove("metadata")

        return LogEntry(
            timestamp = LogTimestamp(timestampRaw),
            level = finalLevel,
            content = LogContent(finalContent.trim()),
            fields = updatedFields,
            instant = timestampParser.parse(timestampRaw)
        ).right()
    }
}
