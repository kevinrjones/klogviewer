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
        isDarkMode: Boolean,
        showAnsiColors: Boolean = true
    ): AnnotatedString {
        return if (showAnsiColors && text.contains("\u001b[")) {
            highlightWithAnsi(text, filterQueries, isDarkMode)
        } else {
            buildAnnotatedString {
                append(text)
                applyOtherHighlights(this, text, filterQueries, isDarkMode)
            }
        }
    }

    private fun highlightWithAnsi(
        text: String,
        filterQueries: List<String>,
        isDarkMode: Boolean
    ): AnnotatedString {
        val ansiRegex = Regex("""\u001b\[([0-9;]*)m""")
        var lastIndex = 0
        var currentStyle = SpanStyle()
        val builder = AnnotatedString.Builder()

        ansiRegex.findAll(text).forEach { match ->
            val plainText = text.substring(lastIndex, match.range.first)
            if (plainText.isNotEmpty()) {
                builder.pushStyle(currentStyle)
                builder.append(plainText)
                builder.pop()
            }
            currentStyle = updateStyleFromAnsi(currentStyle, match.groupValues[1], isDarkMode)
            lastIndex = match.range.last + 1
        }

        val remainingText = text.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            builder.pushStyle(currentStyle)
            builder.append(remainingText)
            builder.pop()
        }

        val annotatedString = builder.toAnnotatedString()
        return buildAnnotatedString {
            append(annotatedString)
            applyOtherHighlights(this, annotatedString.text, filterQueries, isDarkMode)
        }
    }

    private fun updateStyleFromAnsi(currentStyle: SpanStyle, codes: String, isDarkMode: Boolean): SpanStyle {
        if (codes.isEmpty() || codes == "0") return SpanStyle()

        var newStyle = currentStyle
        val parts = codes.split(';')
        parts.forEach { part ->
            val code = part.toIntOrNull() ?: return@forEach
            newStyle = when (code) {
                0 -> SpanStyle()
                1 -> newStyle.copy(fontWeight = FontWeight.Bold)
                30 -> newStyle.copy(color = if (isDarkMode) Color.Gray else Color.Black)
                31 -> newStyle.copy(color = if (isDarkMode) Color(0xFFEF5350) else Color.Red)
                32 -> newStyle.copy(color = if (isDarkMode) Color(0xFF66BB6A) else Color(0xFF2E7D32))
                33 -> newStyle.copy(color = if (isDarkMode) Color(0xFFFFCA28) else Color(0xFFF57F17))
                34 -> newStyle.copy(color = if (isDarkMode) Color(0xFF42A5F5) else Color.Blue)
                35 -> newStyle.copy(color = if (isDarkMode) Color(0xFFAB47BC) else Color(0xFF7B1FA2))
                36 -> newStyle.copy(color = if (isDarkMode) Color(0xFF26C6DA) else Color(0xFF0097A7))
                37 -> newStyle.copy(color = if (isDarkMode) Color.White else Color.LightGray)
                // Bright colors
                90 -> newStyle.copy(color = Color.DarkGray)
                91 -> newStyle.copy(color = Color(0xFFFF8A80))
                92 -> newStyle.copy(color = Color(0xFFB9F6CA))
                93 -> newStyle.copy(color = Color(0xFFFFFF8D))
                94 -> newStyle.copy(color = Color(0xFF82B1FF))
                95 -> newStyle.copy(color = Color(0xFFEA80FC))
                96 -> newStyle.copy(color = Color(0xFF84FFFF))
                97 -> newStyle.copy(color = Color.White)
                else -> newStyle
            }
        }
        return newStyle
    }

    private fun applyOtherHighlights(
        builder: AnnotatedString.Builder,
        text: String,
        filterQueries: List<String>,
        isDarkMode: Boolean
    ) {
        with(builder) {
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
