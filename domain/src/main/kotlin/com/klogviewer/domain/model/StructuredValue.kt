package com.klogviewer.domain.model

sealed interface StructuredValue {
    data class StringValue(val value: String) : StructuredValue
    data class NumberValue(val value: String) : StructuredValue
    data class BooleanValue(val value: Boolean) : StructuredValue
    data object NullValue : StructuredValue
    data class ObjectValue(val fields: Map<String, StructuredValue>) : StructuredValue
    data class ArrayValue(val values: List<StructuredValue>) : StructuredValue
}

typealias StructuredPathIndex = Map<String, List<StructuredValue>>

fun StructuredValue.flattenToPathIndex(): StructuredPathIndex {
    val accumulator = linkedMapOf<String, MutableList<StructuredValue>>()
    flattenValue(
        value = this,
        indexedPath = "",
        anyMatchPath = null,
        accumulator = accumulator
    )
    return accumulator.mapValues { (_, values) -> values.toList() }
}

fun StructuredValue.asDisplayString(): String {
    return when (this) {
        is StructuredValue.StringValue -> value
        is StructuredValue.NumberValue -> value
        is StructuredValue.BooleanValue -> value.toString()
        StructuredValue.NullValue -> "null"
        is StructuredValue.ObjectValue -> {
            fields.keys.sorted().joinToString(prefix = "{", postfix = "}", separator = ",") { key ->
                val escapedKey = key.replace("\\", "\\\\").replace("\"", "\\\"")
                val child = fields.getValue(key)
                "\"$escapedKey\":${child.asJsonLikeValueString()}"
            }
        }

        is StructuredValue.ArrayValue -> {
            values.joinToString(prefix = "[", postfix = "]", separator = ",") { value ->
                value.asJsonLikeValueString()
            }
        }
    }
}

private fun flattenValue(
    value: StructuredValue,
    indexedPath: String,
    anyMatchPath: String?,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
) {
    when (value) {
        is StructuredValue.StringValue,
        is StructuredValue.NumberValue,
        is StructuredValue.BooleanValue,
        StructuredValue.NullValue -> {
            appendPathValue(path = indexedPath, value = value, accumulator = accumulator)
            anyMatchPath?.let { appendPathValue(path = it, value = value, accumulator = accumulator) }
        }

        is StructuredValue.ObjectValue -> {
            value.fields.keys.sorted().forEach { key ->
                val escapedSegment = escapePathSegment(key)
                val childIndexedPath = indexedPath.appendPathSegment(escapedSegment)
                val childAnyMatchPath = anyMatchPath?.appendPathSegment(escapedSegment)
                flattenValue(
                    value = value.fields.getValue(key),
                    indexedPath = childIndexedPath,
                    anyMatchPath = childAnyMatchPath,
                    accumulator = accumulator
                )
            }
        }

        is StructuredValue.ArrayValue -> {
            value.values.forEachIndexed { index, arrayElement ->
                val indexedElementPath = if (indexedPath.isBlank()) {
                    "[$index]"
                } else {
                    "$indexedPath[$index]"
                }
                val anyMatchElementPath = if (indexedPath.isBlank()) {
                    "[]"
                } else {
                    "$indexedPath[]"
                }
                flattenValue(
                    value = arrayElement,
                    indexedPath = indexedElementPath,
                    anyMatchPath = anyMatchElementPath,
                    accumulator = accumulator
                )
            }
        }
    }
}

private fun appendPathValue(
    path: String,
    value: StructuredValue,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
) {
    if (path.isBlank()) {
        return
    }
    accumulator.getOrPut(path) { mutableListOf() }.add(value)
}

private fun String.appendPathSegment(segment: String): String {
    return if (isBlank()) segment else "$this.$segment"
}

private fun escapePathSegment(segment: String): String {
    return segment
        .replace("\\", "\\\\")
        .replace(".", "\\.")
        .replace("[", "\\[")
        .replace("]", "\\]")
}

private fun StructuredValue.asJsonLikeValueString(): String {
    return when (this) {
        is StructuredValue.StringValue -> {
            val escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            "\"$escaped\""
        }

        is StructuredValue.NumberValue -> value
        is StructuredValue.BooleanValue -> value.toString()
        StructuredValue.NullValue -> "null"
        is StructuredValue.ObjectValue -> asDisplayString()
        is StructuredValue.ArrayValue -> asDisplayString()
    }
}
