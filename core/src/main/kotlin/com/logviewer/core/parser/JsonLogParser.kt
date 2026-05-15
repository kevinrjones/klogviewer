package com.logviewer.core.parser

import com.logviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

data class JsonMapping(
    val timestampKey: String = "timestamp",
    val levelKey: String = "level",
    val contentKey: String = "message",
    val timestampPattern: String? = null,
    val levelMapper: LevelMapper = LevelMapper()
)

class JsonLogParser(private val mapping: JsonMapping = JsonMapping()) : LogParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val timestampParser = mapping.timestampPattern?.let { TimestampParser(it) }

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        return try {
            val element = json.parseToJsonElement(line).jsonObject
            
            val timestampRaw = element[mapping.timestampKey]?.toValueString() ?: ""
            val levelRaw = element[mapping.levelKey]?.toValueString()
            val contentElement = element[mapping.contentKey]
            
            val content = when (contentElement) {
                is JsonPrimitive -> contentElement.content
                is JsonObject, is JsonArray -> contentElement.toString()
                null -> ""
            }

            LogEntry(
                timestamp = LogTimestamp(timestampRaw),
                level = mapping.levelMapper.map(levelRaw),
                content = LogContent(content),
                instant = timestampParser?.parse(timestampRaw)
            ).right()
        } catch (e: Exception) {
            logger.debug { "Failed to parse JSON log line: $line" }
            LogFailure.ParsingError("Could not parse JSON log line", line).left()
        }
    }

    private fun JsonElement.toValueString(): String {
        return if (this is JsonPrimitive) this.content else this.toString()
    }
}
