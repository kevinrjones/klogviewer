package com.klogviewer.core.parser

import com.klogviewer.domain.model.StructuredValue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull

internal fun JsonElement.toStructuredValue(): StructuredValue {
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

internal fun JsonElement.toValueString(): String {
    return if (this is JsonPrimitive) content else toString()
}

internal fun JsonElement?.nonNullOrNull(): JsonElement? {
    return this?.takeUnless { element -> element is JsonNull }
}
