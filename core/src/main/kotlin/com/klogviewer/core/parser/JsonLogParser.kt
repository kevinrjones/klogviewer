package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
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

            val mappedLevel = mapping.levelMapper.map(levelRaw)
            val fields = mutableMapOf(
                "timestamp" to timestampRaw,
                "level" to (levelRaw ?: mappedLevel.name),
                "content" to content
            )
            element.forEach { (key, value) ->
                if (key != mapping.timestampKey && key != mapping.levelKey && key != mapping.contentKey) {
                    fields[key] = value.toValueString()
                }
            }

            LogEntry(
                timestamp = LogTimestamp(timestampRaw),
                level = mappedLevel,
                content = LogContent(content),
                fields = fields,
                instant = timestampParser?.parse(timestampRaw),
                structuredData = StructuredLogData(
                    root = element.toStructuredObjectValue(),
                    rawPayload = line
                )
            ).right()
        } catch (_: Exception) {
            logger.debug { "Failed to parse JSON log line: $line" }
            LogFailure.ParsingError("Could not parse JSON log line", line).left()
        }
    }

    private fun JsonElement.toValueString(): String {
        return if (this is JsonPrimitive) this.content else this.toString()
    }

    private fun JsonObject.toStructuredObjectValue(): StructuredValue.ObjectValue {
        return StructuredValue.ObjectValue(
            fields = entries.associate { (key, value) ->
                key to value.toStructuredValue()
            }
        )
    }

    private fun JsonElement.toStructuredValue(): StructuredValue {
        return when (this) {
            is JsonObject -> StructuredValue.ObjectValue(
                fields = entries.associate { (key, value) ->
                    key to value.toStructuredValue()
                }
            )

            is JsonArray -> StructuredValue.ArrayValue(
                values = map { element -> element.toStructuredValue() }
            )

            is JsonPrimitive -> when {
                this is JsonNull -> StructuredValue.NullValue
                booleanOrNull != null -> StructuredValue.BooleanValue(value = boolean)
                isString -> StructuredValue.StringValue(value = content)
                else -> StructuredValue.NumberValue(value = content)
            }
        }
    }
}
