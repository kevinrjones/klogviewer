package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.theme.LogLevelColors

@Composable
fun LogEntryCell(
    column: String,
    entry: LogEntry,
    columnModifier: Modifier,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    showAnsiColors: Boolean,
    sourceIds: List<String>,
    missingSourceIds: Set<String>,
    logColors: LogLevelColors,
    logFontStyle: TextStyle,
    useCompactCellMode: Boolean = true,
    onCellValueClick: (column: String, entry: LogEntry) -> Unit = { _, _ -> }
) {
    val maxLines = if (useCompactCellMode) 1 else Int.MAX_VALUE
    val overflow = if (useCompactCellMode) TextOverflow.Ellipsis else TextOverflow.Visible

    when (column) {
        "Source" -> SourceCell(entry, sourceIds, columnModifier, logFontStyle)
        "Timestamp" -> TimestampCell(entry, columnModifier, logFontStyle, maxLines, overflow)
        "Level" -> LevelCell(entry, columnModifier, logFontStyle, logColors, maxLines, overflow)
        "Message", "Content" -> MessageContentCell(
            column = column,
            entry = entry,
            columnModifier = columnModifier,
            filterQueries = filterQueries,
            isDarkMode = isDarkMode,
            showAnsiColors = showAnsiColors,
            missingSourceIds = missingSourceIds,
            logFontStyle = logFontStyle,
            maxLines = maxLines,
            overflow = overflow,
            useCompactCellMode = useCompactCellMode,
            onCellValueClick = onCellValueClick
        )
        else -> CustomColumnCell(
            column = column,
            entry = entry,
            columnModifier = columnModifier,
            logFontStyle = logFontStyle,
            maxLines = maxLines,
            overflow = overflow,
            useCompactCellMode = useCompactCellMode,
            onCellValueClick = onCellValueClick
        )
    }
}

@Composable
private fun TimestampCell(
    entry: LogEntry,
    columnModifier: Modifier,
    logFontStyle: TextStyle,
    maxLines: Int,
    overflow: TextOverflow
) {
    Text(
        text = entry.timestamp.value,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        style = MaterialTheme.typography.caption.copy(
            fontFamily = logFontStyle.fontFamily,
            fontSize = logFontStyle.fontSize
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = columnModifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun LevelCell(
    entry: LogEntry,
    columnModifier: Modifier,
    logFontStyle: TextStyle,
    logColors: LogLevelColors,
    maxLines: Int,
    overflow: TextOverflow
) {
    val displayLevel = entry.fields["level"]
        ?.takeIf { it != "UNKNOWN" }
        ?: ""
    val color = if (entry.level == LogLevel.UNKNOWN && displayLevel.isNotBlank()) {
        MaterialTheme.colors.onSurface
    } else {
        getLevelColor(entry.level, logColors)
    }
    Text(
        text = displayLevel,
        color = color,
        style = MaterialTheme.typography.caption.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = logFontStyle.fontFamily,
            fontSize = logFontStyle.fontSize
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = columnModifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun MessageContentCell(
    column: String,
    entry: LogEntry,
    columnModifier: Modifier,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    showAnsiColors: Boolean,
    missingSourceIds: Set<String>,
    logFontStyle: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    useCompactCellMode: Boolean,
    onCellValueClick: (column: String, entry: LogEntry) -> Unit
) {
    val fullMessage = if (column == "Message") entry.content.value else entry.fields["content"] ?: entry.content.value
    val displayMessage = if (fullMessage.length > 10000) fullMessage.take(10000) + "..." else fullMessage
    val isMissing = entry.sourceId != null && missingSourceIds.contains(entry.sourceId)
    val highlightedText = LogHighlighter.highlight(displayMessage, filterQueries, isDarkMode, showAnsiColors)
    val cellModifier = if (useCompactCellMode && displayMessage.isNotEmpty()) {
        columnModifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onCellValueClick(column, entry) }
    } else {
        columnModifier.padding(horizontal = 4.dp, vertical = 4.dp)
    }
    Text(
        text = highlightedText,
        style = MaterialTheme.typography.body1.copy(
            fontFamily = logFontStyle.fontFamily,
            fontSize = logFontStyle.fontSize,
            textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = cellModifier
    )
}

@Composable
private fun CustomColumnCell(
    column: String,
    entry: LogEntry,
    columnModifier: Modifier,
    logFontStyle: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    useCompactCellMode: Boolean,
    onCellValueClick: (column: String, entry: LogEntry) -> Unit
) {
    val value = resolveCustomColumnValue(column, entry)
    val cellModifier = if (useCompactCellMode && value.isNotEmpty()) {
        columnModifier
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clickable { onCellValueClick(column, entry) }
    } else {
        columnModifier.padding(horizontal = 4.dp, vertical = 4.dp)
    }
    Text(
        text = value,
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.caption.copy(
            fontFamily = logFontStyle.fontFamily,
            fontSize = logFontStyle.fontSize
        ),
        maxLines = maxLines,
        overflow = overflow,
        modifier = cellModifier
    )
}

internal fun resolveCellValue(column: String, entry: LogEntry): String {
    return when (column) {
        "Timestamp" -> entry.timestamp.value
        "Level" -> entry.fields["level"]?.takeIf { it != "UNKNOWN" } ?: ""
        "Source" -> entry.sourceId ?: ""
        "Line #" -> ""
        "Message", "Content" -> {
            if (column == "Message") entry.content.value
            else entry.fields["content"] ?: entry.content.value
        }
        else -> resolveCustomColumnValue(column, entry)
    }
}
