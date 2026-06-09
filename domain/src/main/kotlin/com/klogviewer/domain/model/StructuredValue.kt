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

data class StructuredFlattenLimits(
    val maxDepth: Int = 32,
    val maxArrayBreadth: Int = 256,
    val maxIndexedPaths: Int = 5_000
)

private const val STRUCTURED_LIMIT_MARKER = "(limit-exceeded)"

fun StructuredValue.flattenToPathIndex(
    limits: StructuredFlattenLimits = StructuredFlattenLimits()
): StructuredPathIndex {
    val accumulator = linkedMapOf<String, MutableList<StructuredValue>>()
    val didTruncate = flattenValue(
        value = this,
        indexedPath = "",
        anyMatchPath = null,
        depth = 0,
        limits = limits,
        accumulator = accumulator
    )
    if (didTruncate) {
        accumulator.getOrPut("_meta.limit") { mutableListOf() }
            .add(StructuredValue.StringValue(STRUCTURED_LIMIT_MARKER))
    }
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
    depth: Int,
    limits: StructuredFlattenLimits,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
): Boolean {
    if (depth > limits.maxDepth || accumulator.size >= limits.maxIndexedPaths) {
        return true
    }

    return when (value) {
        is StructuredValue.StringValue,
        is StructuredValue.NumberValue,
        is StructuredValue.BooleanValue,
        StructuredValue.NullValue -> flattenScalarValue(
            value = value,
            indexedPath = indexedPath,
            anyMatchPath = anyMatchPath,
            limits = limits,
            accumulator = accumulator
        )

        is StructuredValue.ObjectValue -> flattenObjectValue(
            value = value,
            indexedPath = indexedPath,
            anyMatchPath = anyMatchPath,
            depth = depth,
            limits = limits,
            accumulator = accumulator
        )

        is StructuredValue.ArrayValue -> flattenArrayValue(
            value = value,
            indexedPath = indexedPath,
            depth = depth,
            limits = limits,
            accumulator = accumulator
        )
    }
}

private fun flattenScalarValue(
    value: StructuredValue,
    indexedPath: String,
    anyMatchPath: String?,
    limits: StructuredFlattenLimits,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
): Boolean {
    val indexedTruncated = appendPathValue(
        path = indexedPath,
        value = value,
        limits = limits,
        accumulator = accumulator
    )
    val anyMatchTruncated = anyMatchPath?.let { path ->
        appendPathValue(path = path, value = value, limits = limits, accumulator = accumulator)
    } ?: false
    return indexedTruncated || anyMatchTruncated
}

private fun flattenObjectValue(
    value: StructuredValue.ObjectValue,
    indexedPath: String,
    anyMatchPath: String?,
    depth: Int,
    limits: StructuredFlattenLimits,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
): Boolean {
    var truncated = false
    value.fields.keys.sorted().forEach { key ->
        if (truncated) {
            return@forEach
        }

        val escapedSegment = escapePathSegment(key)
        val childIndexedPath = indexedPath.appendPathSegment(escapedSegment)
        val childAnyMatchPath = anyMatchPath?.appendPathSegment(escapedSegment)
        truncated = flattenValue(
            value = value.fields.getValue(key),
            indexedPath = childIndexedPath,
            anyMatchPath = childAnyMatchPath,
            depth = depth + 1,
            limits = limits,
            accumulator = accumulator
        ) || truncated
    }
    return truncated
}

private fun flattenArrayValue(
    value: StructuredValue.ArrayValue,
    indexedPath: String,
    depth: Int,
    limits: StructuredFlattenLimits,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
): Boolean {
    val hasOverflowValues = value.values.size > limits.maxArrayBreadth
    var truncated = false
    value.values.take(limits.maxArrayBreadth).forEachIndexed { index, arrayElement ->
        if (truncated) {
            return@forEachIndexed
        }
        val childIndexedPath = if (indexedPath.isBlank()) {
            "[$index]"
        } else {
            "$indexedPath[$index]"
        }
        val childAnyMatchPath = if (indexedPath.isBlank()) {
            "[]"
        } else {
            "$indexedPath[]"
        }
        truncated = flattenValue(
            value = arrayElement,
            indexedPath = childIndexedPath,
            anyMatchPath = childAnyMatchPath,
            depth = depth + 1,
            limits = limits,
            accumulator = accumulator
        ) || truncated
    }
    return truncated || hasOverflowValues
}

private fun appendPathValue(
    path: String,
    value: StructuredValue,
    limits: StructuredFlattenLimits,
    accumulator: MutableMap<String, MutableList<StructuredValue>>
): Boolean {
    val exceedsIndexLimit =
        path.isNotBlank() && !accumulator.containsKey(path) && accumulator.size >= limits.maxIndexedPaths
    if (path.isBlank() || exceedsIndexLimit) {
        return exceedsIndexLimit
    }
    accumulator.getOrPut(path) { mutableListOf() }.add(value)
    return false
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
