package com.klogviewer.domain.model

data class StructuredLogData(
    val root: StructuredValue.ObjectValue,
    val flatPathIndex: StructuredPathIndex = root.flattenToPathIndex(),
    val rawPayload: String? = null,
    val canonicalFields: Map<String, StructuredValue> = emptyMap()
) {
    fun toCompatibilityFields(): Map<String, String> {
        val flattenedProjection = flatPathIndex
            .toSortedMap()
            .mapValues { (_, values) -> values.toCompatibilityString() }

        val canonicalProjection = canonicalFields
            .toSortedMap()
            .mapValues { (_, value) -> value.asDisplayString() }

        return flattenedProjection + canonicalProjection
    }
}

private fun List<StructuredValue>.toCompatibilityString(): String {
    return if (size == 1) {
        first().asDisplayString()
    } else {
        joinToString(separator = ",") { value -> value.asDisplayString() }
    }
}
