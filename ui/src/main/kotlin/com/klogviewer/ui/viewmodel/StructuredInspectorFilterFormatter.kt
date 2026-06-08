package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.domain.model.asDisplayString

private const val ESCAPE_BUFFER_PADDING = 8

object StructuredInspectorFilterFormatter {
    fun fieldPredicate(path: String): String {
        val normalizedPath = normalizePath(path) ?: return ""
        return "field:$normalizedPath exists"
    }

    fun valuePredicate(path: String, value: StructuredValue): String {
        val normalizedPath = normalizePath(path) ?: return ""
        return "field:$normalizedPath=${formatLiteral(value)}"
    }

    private fun normalizePath(path: String): String? {
        val trimmedPath = path.trim()
        return trimmedPath.takeIf { it.isNotEmpty() }
    }

    private fun formatLiteral(value: StructuredValue): String {
        return when (value) {
            is StructuredValue.StringValue -> "\"${escapeString(value.value)}\""
            is StructuredValue.NumberValue -> value.value
            is StructuredValue.BooleanValue -> value.value.toString()
            StructuredValue.NullValue -> "null"
            else -> "\"${escapeString(value.asDisplayString())}\""
        }
    }

    private fun escapeString(value: String): String {
        return buildString(value.length + ESCAPE_BUFFER_PADDING) {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
