package com.klogviewer.core.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal fun deriveJsonColumns(mapping: JsonMapping, firstJson: JsonObject?): List<String> {
    if (firstJson == null) return listOf("Timestamp", "Level", "Content")

    val keys = firstJson.keys.toMutableSet()
    val resultColumns = mutableListOf<String>()

    if (keys.remove(mapping.timestampKey)) resultColumns.add("Timestamp")
    if (keys.remove(mapping.levelKey)) resultColumns.add("Level")
    if (keys.remove(mapping.contentKey)) resultColumns.add("Content")

    resultColumns.addAll(keys.sorted().map { it.replaceFirstChar { c -> c.uppercase() } })
    return resultColumns
}

internal fun detectJsonMapping(parsedObjects: List<JsonObject>): JsonMapping {
    val keys = parsedObjects
        .flatMap { it.keys }
        .toSet()
        .ifEmpty { return JsonMapping() }

    val timestampKey = keys.firstAvailableKey(CanonicalFieldAliases.TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER)
    val levelKey = keys.firstAvailableKey(CanonicalFieldAliases.LEVEL_ALIASES_IN_PRECEDENCE_ORDER)
    val contentKey = keys.firstAvailableKey(CanonicalFieldAliases.CONTENT_KEYS_IN_PRECEDENCE_ORDER)

    return JsonMapping(timestampKey, levelKey, contentKey)
}

internal fun Set<String>.firstAvailableKey(candidates: List<String>): String =
    candidates.firstOrNull { it in this } ?: candidates.first()

internal fun isLogfmt(line: String): Boolean {
    val trimmed = line.trim()
    val regex = """\w+=(?:"[^"]*"|\S+)""".toRegex()
    val matches = regex.findAll(trimmed).toList()
    return matches.size >= 2 || (matches.size == 1 && trimmed.startsWith(matches[0].value))
}

internal fun collectJsonSamples(lines: List<String>): JsonSampleStats {
    val parsedObjects = mutableListOf<JsonObject>()
    var malformedCount = 0

    lines.forEach { line ->
        val trimmed = line.trim()
        if (looksJsonLike(trimmed)) {
            val parsed = runCatching { Json.parseToJsonElement(trimmed) }.getOrNull()
            if (parsed is JsonObject) {
                parsedObjects.add(parsed)
            } else if (parsed == null) {
                malformedCount += 1
            }
        }
    }

    return JsonSampleStats(parsedObjects = parsedObjects, malformedCount = malformedCount)
}

internal fun looksJsonLike(trimmedLine: String): Boolean {
    val startsLikeJson = trimmedLine.startsWith("{") || trimmedLine.startsWith("[")
    val endsLikeJson = trimmedLine.endsWith("}") || trimmedLine.endsWith("]")
    return startsLikeJson || endsLikeJson
}

internal data class JsonDetectionAnalysis(
    val parsedJsonObjects: List<JsonObject>,
    val confidence: ParseDetectionConfidence,
    val shouldSelectJson: Boolean
)

internal data class JsonSampleStats(
    val parsedObjects: List<JsonObject>,
    val malformedCount: Int
)
