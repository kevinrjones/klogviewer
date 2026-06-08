package com.klogviewer.core.parser

import com.klogviewer.domain.model.StructuredValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

internal data class NestedJsonScopeExtraction(
    val decodedScopes: Map<String, JsonObject>,
    val derivedNamespaceKey: String?
) {
    val fallbackScopes: List<JsonObject>
        get() = decodedScopes.values.toList()

    fun derivedStructuredFieldOrNull(): StructuredValue.ObjectValue? {
        if (decodedScopes.isEmpty()) {
            return null
        }

        return StructuredValue.ObjectValue(
            fields = decodedScopes
                .mapValues { (_, value) -> value.toStructuredValue() }
        )
    }
}

internal class NestedJsonScopeExtractor(
    private val json: Json,
    private val nestedScopeKeys: List<String> = DEFAULT_NESTED_JSON_SCOPE_KEYS,
    private val namespaceCandidates: List<String> = DEFAULT_NAMESPACE_CANDIDATES
) {
    fun extract(source: JsonObject): NestedJsonScopeExtraction {
        val decodedScopes = nestedScopeKeys
            .mapNotNull { key ->
                source[key].toNestedJsonObjectOrNull()?.let { jsonObject -> key to jsonObject }
            }
            .toMap()

        return NestedJsonScopeExtraction(
            decodedScopes = decodedScopes,
            derivedNamespaceKey = if (decodedScopes.isEmpty()) {
                null
            } else {
                resolveDerivedNamespaceKey(source)
            }
        )
    }

    private fun resolveDerivedNamespaceKey(source: JsonObject): String {
        val firstAvailable = namespaceCandidates.firstOrNull { candidate -> !source.containsKey(candidate) }
        if (firstAvailable != null) {
            return firstAvailable
        }

        var suffix = 1
        while (source.containsKey("${namespaceCandidates.last()}_$suffix")) {
            suffix += 1
        }
        return "${namespaceCandidates.last()}_$suffix"
    }

    private fun JsonElement?.toNestedJsonObjectOrNull(): JsonObject? {
        return when (this) {
            null -> null
            is JsonObject -> this
            is JsonPrimitive -> {
                if (!isString) {
                    null
                } else {
                    content.parseJsonObjectOrNull()
                }
            }

            is JsonArray -> null
        }
    }

    private fun String.parseJsonObjectOrNull(): JsonObject? {
        val trimmed = trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }

        return runCatching {
            json.parseToJsonElement(trimmed).jsonObject
        }.getOrNull()
    }

    private companion object {
        private val DEFAULT_NESTED_JSON_SCOPE_KEYS = listOf(
            "log",
            "body",
            "event",
            "jsonPayload",
            "payload",
            "record"
        )

        private val DEFAULT_NAMESPACE_CANDIDATES = listOf(
            "_decoded",
            "_decoded_derived",
            "_klogviewer_decoded"
        )
    }
}
