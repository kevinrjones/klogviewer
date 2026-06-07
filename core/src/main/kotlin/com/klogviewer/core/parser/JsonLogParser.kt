package com.klogviewer.core.parser

import com.klogviewer.domain.parser.LogParser
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.SerializationException
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

            val rootStructuredValue = element.toStructuredObjectValue()
            val canonicalFields = element.toCanonicalFields()

            val timestampRaw = element[mapping.timestampKey].nonNullOrNull()?.toValueString()
                ?: canonicalFields[CanonicalFieldAliases.CANONICAL_TIMESTAMP]?.asDisplayString()
                ?: ""
            val levelRaw = element[mapping.levelKey].nonNullOrNull()?.toValueString()
                ?: canonicalFields[CanonicalFieldAliases.CANONICAL_LEVEL]?.asDisplayString()
            val contentElement = element[mapping.contentKey].nonNullOrNull()

            val content = when (contentElement) {
                is JsonPrimitive -> contentElement.content
                is JsonObject, is JsonArray -> contentElement.toString()
                null -> canonicalFields[CanonicalFieldAliases.CANONICAL_MESSAGE]?.asDisplayString().orEmpty()
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
                    root = rootStructuredValue,
                    rawPayload = line,
                    canonicalFields = canonicalFields
                )
            ).right()
        } catch (e: SerializationException) {
            logger.debug(e) { "Failed to parse JSON log line: $line" }
            LogFailure.ParsingError("Could not parse JSON log line", line).left()
        } catch (e: IllegalArgumentException) {
            logger.debug(e) { "Failed to parse JSON log line: $line" }
            LogFailure.ParsingError("Could not parse JSON log line", line).left()
        }
    }

    private fun JsonElement.toValueString(): String {
        return if (this is JsonPrimitive) this.content else this.toString()
    }

    private fun JsonElement?.nonNullOrNull(): JsonElement? {
        return this?.takeUnless { element -> element is JsonNull }
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

    private fun JsonObject.toCanonicalFields(): Map<String, StructuredValue> {
        return linkedMapOf<String, StructuredValue>().apply {
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_TIMESTAMP,
                CanonicalFieldAliases.TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_LEVEL,
                CanonicalFieldAliases.LEVEL_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_MESSAGE,
                CanonicalFieldAliases.MESSAGE_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_LOGGER,
                CanonicalFieldAliases.LOGGER_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_EXCEPTION,
                CanonicalFieldAliases.EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_TRACE_ID,
                CanonicalFieldAliases.TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER
            )
            putCanonicalValue(
                this@toCanonicalFields,
                CanonicalFieldAliases.CANONICAL_SPAN_ID,
                CanonicalFieldAliases.SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER
            )
        }
    }

    private fun MutableMap<String, StructuredValue>.putCanonicalValue(
        source: JsonObject,
        canonicalKey: String,
        aliasesInOrder: List<String>
    ) {
        val value = source.firstNonNullAliasValue(aliasesInOrder) ?: return
        put(canonicalKey, value.toStructuredValue())
    }

    private fun JsonObject.firstNonNullAliasValue(aliasesInOrder: List<String>): JsonElement? {
        return aliasesInOrder
            .asSequence()
            .mapNotNull { alias -> this[alias].nonNullOrNull() }
            .firstOrNull()
    }

}
