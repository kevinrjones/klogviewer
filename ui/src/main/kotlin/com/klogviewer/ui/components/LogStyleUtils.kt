package com.klogviewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.theme.LogLevelColors

private const val MIN_FONT_SIZE_SP = 8
private const val MAX_FONT_SIZE_SP = 72

internal fun createLogFontStyle(fontFamily: String, fontSizeSp: Int): TextStyle {
    return TextStyle(
        fontFamily = resolveMonospacedFontFamily(fontFamily),
        fontSize = fontSizeSp.coerceIn(MIN_FONT_SIZE_SP, MAX_FONT_SIZE_SP).sp
    )
}

internal fun resolveMonospacedFontFamily(fontFamily: String): FontFamily {
    return when (fontFamily.lowercase()) {
        "monospaced", "monospace", "dialoginput" -> FontFamily.Monospace
        else -> FontFamily.Monospace
    }
}

internal fun getLevelColor(level: LogLevel, colors: LogLevelColors): Color = when (level) {
    LogLevel.TRACE -> colors.trace
    LogLevel.DEBUG -> colors.debug
    LogLevel.INFO -> colors.info
    LogLevel.WARN -> colors.warn
    LogLevel.ERROR -> colors.error
    LogLevel.FATAL -> colors.fatal
    LogLevel.UNKNOWN -> colors.unknown
}

internal fun resolveCustomColumnValue(column: String, entry: LogEntry): String {
    val fields = entry.compatibilityFields()
    val normalizedColumn = column.normalizedFieldLookupKey()
    val snakeCaseColumn = column.lowercase().replace(" ", "_")

    return fields[column]
        ?: fields[snakeCaseColumn]
        ?: fields.entries.firstOrNull { entry -> entry.key.equals(column, ignoreCase = true) }?.value
        ?: fields.entries.firstOrNull { entry ->
            entry.key.normalizedFieldLookupKey() == normalizedColumn
        }?.value
        ?: ""
}

private fun String.normalizedFieldLookupKey(): String {
    return lowercase().filter { it.isLetterOrDigit() }
}
