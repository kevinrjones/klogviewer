package com.klogviewer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.theme.LogLevelColors
import kotlin.math.roundToInt

internal const val MAX_DEFAULT_COLUMN_WIDTH = 300
internal const val DEFAULT_MESSAGE_COLUMN_WIDTH = 1200

private const val MIN_HEADER_CHAR_WIDTH_DP = 6f
private const val HEADER_CHAR_WIDTH_FACTOR = 0.75f
private const val HEADER_EXTRA_PADDING_DP = 28

private const val MIN_CELL_CHAR_WIDTH_DP = 5f
private const val CELL_CHAR_WIDTH_FACTOR = 0.62f
private const val COLUMN_CONTENT_PADDING_DP = 20

private const val GUTTER_BASE_PADDING_DP = 16
private const val GUTTER_EMPTY_SINGLE_SOURCE_WIDTH = 50
private const val GUTTER_EMPTY_MULTI_SOURCE_WIDTH = 68
private const val TIMESTAMP_EMPTY_WIDTH = 180
private const val LEVEL_EMPTY_WIDTH = 80
private const val SOURCE_EMPTY_WIDTH = 140
private const val MESSAGE_EMPTY_WIDTH = 300
private const val CUSTOM_EMPTY_WIDTH = 120

private const val MIN_LINE_NUMBER_DIGITS = 4
private const val MAX_MESSAGE_CHAR_INSPECT = 10000

private const val BADGE_PADDING_DP = 14
private const val AUTO_FIT_MIN_WIDTH_DP = 40
private const val AUTO_FIT_MAX_WIDTH_DP = 2000

internal fun getLogListContentWidth(
    columns: List<String>,
    columnWidths: Map<String, Int>,
    gutterWidth: Dp
): Dp = columns.fold(gutterWidth) { total, column ->
    total + getColumnWidth(column, columnWidths)
}

internal fun getColumnWidth(column: String, columnWidths: Map<String, Int>, sourceIds: List<String> = emptyList()): Dp {
    val width = columnWidths[column]
    if (width != null) return width.dp

    val defaultWidth = when (column) {
        "Line #", "#" -> if (sourceIds.size > 1) 68.dp else 50.dp
        "Timestamp" -> 180.dp
        "Level" -> 80.dp
        "Source" -> 140.dp
        "Message", "Content" -> DEFAULT_MESSAGE_COLUMN_WIDTH.dp
        else -> 120.dp
    }

    return defaultWidth.coerceAtMost(MAX_DEFAULT_COLUMN_WIDTH.dp)
}

private fun calculateHeaderWidth(column: String, logFontSizeSp: Int): Int {
    val headerTitle = if (column == "Line #" || column == "#") "#" else column
    val headerCharWidthDp = maxOf(MIN_HEADER_CHAR_WIDTH_DP, logFontSizeSp * HEADER_CHAR_WIDTH_FACTOR)
    return (headerTitle.length * headerCharWidthDp).roundToInt() + HEADER_EXTRA_PADDING_DP
}

private fun calculateDefaultEmptyColumnWidth(column: String, sourceIdsCount: Int): Int {
    return when (column) {
        "Line #", "#" -> if (sourceIdsCount > 1) GUTTER_EMPTY_MULTI_SOURCE_WIDTH else GUTTER_EMPTY_SINGLE_SOURCE_WIDTH
        "Timestamp" -> TIMESTAMP_EMPTY_WIDTH
        "Level" -> LEVEL_EMPTY_WIDTH
        "Source" -> SOURCE_EMPTY_WIDTH
        "Message", "Content" -> MESSAGE_EMPTY_WIDTH
        else -> CUSTOM_EMPTY_WIDTH
    }
}

private fun extractLevelDisplayLength(entry: LogEntry): Int {
    val levelStr = entry.fields["level"]?.takeIf { it != "UNKNOWN" } ?: entry.level.name
    return levelStr.length
}

private fun extractSourceDisplayLength(entry: LogEntry, displayNames: Map<String, String>): Int {
    val sourceId = entry.sourceId ?: return 0
    val name = displayNames[sourceId] ?: sourceId.extractSourceFileName()
    return name.length
}

private fun extractMessageDisplayLength(entry: LogEntry, column: String): Int {
    val fullMsg = if (column == "Message") {
        entry.content.value
    } else {
        entry.fields["content"] ?: entry.content.value
    }
    val displayMsg = if (fullMsg.length > MAX_MESSAGE_CHAR_INSPECT) fullMsg.take(MAX_MESSAGE_CHAR_INSPECT) else fullMsg
    return displayMsg.lineSequence().maxOfOrNull { it.length } ?: 0
}

private fun extractMaxColumnLength(
    column: String,
    logs: List<LogEntry>,
    displayNames: Map<String, String>
): Int {
    return when (column) {
        "Line #", "#" -> maxOf(MIN_LINE_NUMBER_DIGITS, logs.size.toString().length)
        "Timestamp" -> logs.maxOfOrNull { entry -> entry.timestamp.value.length } ?: 0
        "Level" -> logs.maxOfOrNull { entry -> extractLevelDisplayLength(entry) } ?: 0
        "Source" -> logs.maxOfOrNull { entry -> extractSourceDisplayLength(entry, displayNames) } ?: 0
        "Message", "Content" -> logs.maxOfOrNull { entry -> extractMessageDisplayLength(entry, column) } ?: 0
        else -> logs.maxOfOrNull { entry -> resolveCustomColumnValue(column, entry).length } ?: 0
    }
}

private fun calculateExtraColumnPadding(
    column: String,
    sourceIdsCount: Int,
    hasStructuredData: Boolean
): Int {
    if (column != "Line #" && column != "#") return COLUMN_CONTENT_PADDING_DP

    var padding = GUTTER_BASE_PADDING_DP
    if (sourceIdsCount > 1) padding += BADGE_PADDING_DP
    if (hasStructuredData) padding += BADGE_PADDING_DP
    return padding
}

internal fun calculateColumnWidthToContent(
    column: String,
    logs: List<LogEntry>,
    sourceIds: List<String> = emptyList(),
    logFontSizeSp: Int = 12,
    minWidthDp: Int = AUTO_FIT_MIN_WIDTH_DP,
    maxWidthDp: Int = AUTO_FIT_MAX_WIDTH_DP
): Int {
    val headerWidthDp = calculateHeaderWidth(column, logFontSizeSp)

    if (logs.isEmpty()) {
        val defaultEmptyWidth = calculateDefaultEmptyColumnWidth(column, sourceIds.size)
        return maxOf(headerWidthDp, defaultEmptyWidth).coerceIn(minWidthDp, maxWidthDp)
    }

    val charWidthDp = maxOf(MIN_CELL_CHAR_WIDTH_DP, logFontSizeSp * CELL_CHAR_WIDTH_FACTOR)
    val displayNames = buildSourceDisplayNames(sourceIds)
    val maxContentCharLength = extractMaxColumnLength(column, logs, displayNames)

    val extraPaddingDp = calculateExtraColumnPadding(
        column = column,
        sourceIdsCount = sourceIds.size,
        hasStructuredData = logs.any { it.structuredData != null }
    )

    val contentWidthDp = (maxContentCharLength * charWidthDp).roundToInt() + extraPaddingDp
    return maxOf(headerWidthDp, contentWidthDp).coerceIn(minWidthDp, maxWidthDp)
}
