package com.klogviewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object LogHighlighter {
    private val timestampRegex = Regex("""\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}(?:\.\d+)?""")
    private val ipRegex = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val uuidRegex = Regex("""\b[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}\b""")

    fun highlight(
        text: String,
        filterQueries: List<String>,
        isDarkMode: Boolean
    ): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            // Highlight Filter Queries (Bold & Background)
            filterQueries.forEach { query ->
                if (query.isNotEmpty()) {
                    try {
                        val matches = query.toRegex(RegexOption.IGNORE_CASE).findAll(text)
                        matches.forEach { match ->
                            addStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    background = if (isDarkMode) Color(0xFF424242) else Color(0xFFFFE082)
                                ),
                                match.range.first,
                                match.range.last + 1
                            )
                        }
                    } catch (_: Exception) {
                        // Ignore regex errors in search query
                    }
                }
            }

            // Highlight Timestamps
            timestampRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = if (isDarkMode) Color(0xFF81C784) else Color(0xFF2E7D32)),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Highlight IPs
            ipRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = if (isDarkMode) Color(0xFF64B5F6) else Color(0xFF1565C0)),
                    match.range.first,
                    match.range.last + 1
                )
            }

            // Highlight UUIDs
            uuidRegex.findAll(text).forEach { match ->
                addStyle(
                    SpanStyle(color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFEF6C00)),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}
