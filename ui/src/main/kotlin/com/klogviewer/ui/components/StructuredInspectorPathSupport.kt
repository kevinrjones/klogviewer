package com.klogviewer.ui.components

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.StructuredValue

private const val NODE_INDENT_MULTIPLIER = 14

fun structuredChildren(value: StructuredValue, path: String): List<StructuredChild> {
    return when (value) {
        is StructuredValue.ObjectValue -> value.fields.keys.sorted().map { key ->
            StructuredChild(
                label = key,
                path = path.appendPathSegment(escapePathSegment(key)),
                value = value.fields.getValue(key)
            )
        }

        is StructuredValue.ArrayValue -> value.values.mapIndexed { index, child ->
            StructuredChild(
                label = "[$index]",
                path = if (path.isBlank()) {
                    "[$index]"
                } else {
                    "$path[$index]"
                },
                value = child
            )
        }

        else -> emptyList()
    }
}

fun nodeStartPadding(depth: Int): Dp {
    return (depth * NODE_INDENT_MULTIPLIER).dp
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
