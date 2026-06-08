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
    private val nestedJsonScopeExtractor = NestedJsonScopeExtractor(json = json)
    private val canonicalFieldExtractor = CanonicalFieldExtractor()

    override fun parse(line: String): Either<LogFailure.ParsingError, LogEntry> {
        return try {
            val element = json.parseToJsonElement(line).jsonObject
            val nestedScopeExtraction = nestedJsonScopeExtractor.extract(element)

            val rootStructuredValue = element.toStructuredObjectValue(nestedScopeExtraction)
            val canonicalFields = canonicalFieldExtractor.extract(
                source = element,
                fallbackScopes = nestedScopeExtraction.fallbackScopes
            )

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

    private fun JsonObject.toStructuredObjectValue(
        nestedScopeExtraction: NestedJsonScopeExtraction
    ): StructuredValue.ObjectValue {
        val rawFields = entries.associate { (key, value) ->
            key to value.toStructuredValue()
        }
        val derivedNamespaceKey = nestedScopeExtraction.derivedNamespaceKey
        val derivedNestedField = nestedScopeExtraction.derivedStructuredFieldOrNull()

        return StructuredValue.ObjectValue(
            fields = if (derivedNamespaceKey == null || derivedNestedField == null) {
                rawFields
            } else {
                rawFields + (derivedNamespaceKey to derivedNestedField)
            }
        )
    }
}
