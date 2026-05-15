package com.logviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel
import com.logviewer.ui.theme.LogLevelColors
import com.logviewer.ui.theme.LogViewerTheme

@Composable
fun LogList(
    logs: List<LogEntry>,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    columns: List<String> = emptyList(),
    columnWidths: Map<String, Int> = emptyMap(),
    selectedEntry: LogEntry? = null,
    onEntryClick: (LogEntry) -> Unit = {},
    onColumnResize: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()

    val displayColumns = if (columns.isEmpty()) listOf("Timestamp", "Level", "Message") else columns

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LogListHeader(
                        columns = displayColumns,
                        columnWidths = columnWidths,
                        onColumnResize = onColumnResize,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        state = verticalScrollState,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                    ) {
                        itemsIndexed(logs) { index, entry ->
                            LogEntryRow(
                                entry = entry,
                                lineNumber = index + 1,
                                filterQueries = filterQueries,
                                isDarkMode = isDarkMode,
                                columns = displayColumns,
                                columnWidths = columnWidths,
                                isSelected = entry == selectedEntry,
                                onClick = { onEntryClick(entry) }
                            )
                        }
                    }
                }
                HorizontalScrollbar(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    adapter = rememberScrollbarAdapter(horizontalScrollState)
                )
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().width(8.dp),
                adapter = rememberScrollbarAdapter(verticalScrollState)
            )
        }
    }
}

@Composable
fun LogListHeader(
    columns: List<String>,
    columnWidths: Map<String, Int>,
    onColumnResize: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 1.dp,
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
            )
            
            columns.forEachIndexed { index, column ->
                val isLast = index == columns.lastIndex
                val widthDp = getColumnWidth(column, columnWidths)
                val isResized = columnWidths.containsKey(column)
                val currentWidthState = rememberUpdatedState(widthDp)
                
                val columnModifier = if (isLast && !isResized) {
                    Modifier.weight(1f).widthIn(min = widthDp)
                } else {
                    Modifier.width(widthDp)
                }

                Box(
                    modifier = columnModifier
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .width(8.dp)
                                .fillMaxHeight()
                                .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
                                .pointerInput(column) {
                                    var startWidth = 0.dp
                                    var accumulatedDrag = 0f
                                    detectDragGestures(
                                        onDragStart = { 
                                            startWidth = currentWidthState.value
                                            accumulatedDrag = 0f 
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedDrag += dragAmount.x
                                            val newWidth = (startWidth + with(density) { accumulatedDrag.toDp() }).coerceAtLeast(40.dp)
                                            onColumnResize(column, newWidth.value.toInt())
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryRow(
    entry: LogEntry,
    lineNumber: Int,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    columns: List<String>,
    columnWidths: Map<String, Int>,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val logColors = LogViewerTheme.logColors
    Box(
        modifier = Modifier
            .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Gutter / Line Number
        Text(
            text = lineNumber.toString().padStart(4, ' '),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
        )
        
        columns.forEachIndexed { index, column ->
            val isLast = index == columns.lastIndex
            val widthDp = getColumnWidth(column, columnWidths)
            val isResized = columnWidths.containsKey(column)
            
            val columnModifier = if (isLast && !isResized) {
                Modifier.widthIn(min = widthDp)
            } else {
                Modifier.width(widthDp)
            }

            when (column) {
                "Timestamp" -> {
                    Text(
                        text = entry.timestamp.value,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.caption,
                        modifier = columnModifier.padding(horizontal = 4.dp)
                    )
                }
                "Level" -> {
                    val displayLevel = when {
                        entry.level != LogLevel.UNKNOWN -> "[${entry.level}]"
                        entry.fields["level"] != null && entry.fields["level"] != "UNKNOWN" -> entry.fields["level"]!!
                        else -> ""
                    }
                    val color = if (entry.level == LogLevel.UNKNOWN && entry.fields.containsKey("level") && entry.fields["level"] != "UNKNOWN") {
                        MaterialTheme.colors.onSurface
                    } else {
                        getLevelColor(entry.level, logColors)
                    }
                    Text(
                        text = displayLevel,
                        color = color,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        modifier = columnModifier.padding(horizontal = 4.dp)
                    )
                }
                "Message", "Content" -> {
                    val message = if (column == "Message") entry.content.value else entry.fields["content"] ?: entry.content.value
                    Text(
                        text = LogHighlighter.highlight(message, filterQueries, isDarkMode),
                        style = MaterialTheme.typography.body1,
                        fontSize = 12.sp,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                        modifier = columnModifier.padding(horizontal = 4.dp)
                    )
                }
                else -> {
                    val fieldName = column.lowercase().replace(" ", "_")
                    val value = entry.fields[fieldName] ?: ""
                    Text(
                        text = value,
                        color = MaterialTheme.colors.onSurface,
                        style = MaterialTheme.typography.caption,
                        modifier = columnModifier.padding(horizontal = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
}

private fun getColumnWidth(column: String, columnWidths: Map<String, Int>): Dp {
    val width = columnWidths[column]
    if (width != null) return width.dp
    
    return when (column) {
        "Timestamp" -> 180.dp
        "Level" -> 80.dp
        "Message", "Content" -> 600.dp
        else -> 120.dp
    }
}

private fun getLevelColor(level: LogLevel, colors: LogLevelColors): Color = when (level) {
    LogLevel.DEBUG -> colors.debug
    LogLevel.INFO -> colors.info
    LogLevel.WARN -> colors.warn
    LogLevel.ERROR -> colors.error
    LogLevel.FATAL -> colors.fatal
    LogLevel.UNKNOWN -> colors.unknown
}
