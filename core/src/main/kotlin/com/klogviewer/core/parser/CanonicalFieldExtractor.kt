package com.klogviewer.core.parser

import com.klogviewer.domain.model.StructuredValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal class CanonicalFieldExtractor(
    private val canonicalAliases: Map<String, List<String>> =
        CanonicalFieldAliases.CANONICAL_ALIASES_IN_PRECEDENCE_ORDER
) {
    fun extract(source: JsonObject, fallbackScopes: List<JsonObject>): Map<String, StructuredValue> {
        return canonicalAliases
            .mapNotNull { (canonicalKey, aliasesInOrder) ->
                source.firstNonNullAliasValue(
                    aliasesInOrder = aliasesInOrder,
                    fallbackScopes = fallbackScopes
                )?.let { value ->
                    canonicalKey to value.toStructuredValue()
                }
            }
            .toMap(linkedMapOf())
    }

    private fun JsonObject.firstNonNullAliasValue(
        aliasesInOrder: List<String>,
        fallbackScopes: List<JsonObject>
    ): JsonElement? {
        val rootValue = aliasesInOrder
            .asSequence()
            .mapNotNull { alias -> this[alias].nonNullOrNull() }
            .firstOrNull()
        if (rootValue != null) {
            return rootValue
        }

        return fallbackScopes
            .asSequence()
            .mapNotNull { scope ->
                aliasesInOrder
                    .asSequence()
                    .mapNotNull { alias -> scope[alias].nonNullOrNull() }
                    .firstOrNull()
            }
            .firstOrNull()
    }
}
