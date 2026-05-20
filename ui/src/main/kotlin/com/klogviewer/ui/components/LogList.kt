package com.klogviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.theme.LogLevelColors
import com.klogviewer.ui.theme.KLogViewerTheme
import com.klogviewer.ui.components.TooltipWrapper

@Composable
fun LogList(
    logs: List<LogEntry>,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    sourceIds: List<String> = emptyList(),
    missingSourceIds: Set<String> = emptySet(),
    columns: List<String> = emptyList(),
    columnWidths: Map<String, Int> = emptyMap(),
    isAutoScrollEnabled: Boolean = true,
    showAnsiColors: Boolean = true,
    selectedIndices: Set<Int> = emptySet(),
    onEntryClick: (LogEntry) -> Unit = {},
    onToggleSelection: (Int, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onColumnResize: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()
    val density = LocalDensity.current
    var widestRowWidth by remember(sourceIds) { mutableStateOf(0.dp) }

    LaunchedEffect(logs.size) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            verticalScrollState.scrollToItem(logs.size - 1)
        }
    }

    val displayColumns = if (columns.isEmpty()) listOf("Timestamp", "Level", "Message") else columns

    BoxWithConstraints(modifier = modifier.fillMaxSize().testTag("log_list")) {
        val viewportWidth = maxWidth
        val minContentWidth = if (widestRowWidth > viewportWidth) widestRowWidth else viewportWidth
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
                        viewportWidth = minContentWidth,
                        sourceIds = sourceIds,
                        onColumnResize = onColumnResize,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        state = verticalScrollState,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth().testTag("log_lazy_column")
                    ) {
                        itemsIndexed(logs) { index, entry ->
                            LogEntryRow(
                                entry = entry,
                                lineNumber = index + 1,
                                filterQueries = filterQueries,
                                isDarkMode = isDarkMode,
                                viewportWidth = minContentWidth,
                                showAnsiColors = showAnsiColors,
                                sourceIds = sourceIds,
                                missingSourceIds = missingSourceIds,
                                columns = displayColumns,
                                columnWidths = columnWidths,
                                isSelected = selectedIndices.contains(index),
                                onClick = { isShift, isMeta -> 
                                    if (isShift || isMeta) {
                                        onToggleSelection(index, isShift, isMeta)
                                    } else {
                                        onEntryClick(entry)
                                    }
                                },
                                modifier = Modifier.onSizeChanged { size ->
                                    val width = with(density) { size.width.toDp() }
                                    if (width > widestRowWidth) {
                                        widestRowWidth = width
                                    }
                                }.testTag("log_entry_row")
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
    viewportWidth: Dp,
    sourceIds: List<String> = emptyList(),
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
                .widthIn(min = viewportWidth)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val gutterWidth = if (sourceIds.size > 1) 60.dp else 50.dp
            Text(
                text = "#",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(gutterWidth).padding(horizontal = 4.dp)
            )
            
            columns.forEachIndexed { index, column ->
                val isLast = index == columns.lastIndex
                val widthDp = getColumnWidth(column, columnWidths)
                val isResized = columnWidths.containsKey(column)
                val currentWidthState = rememberUpdatedState(widthDp)
                
                val columnModifier = if (isLast && !isResized) {
                    val otherWidths = columns.dropLast(1).sumOf { getColumnWidth(it, columnWidths).value.toDouble() }.dp + gutterWidth
                    val minWidth = if (viewportWidth - otherWidths > widthDp) viewportWidth - otherWidths else widthDp
                    Modifier.widthIn(min = minWidth, max = 10000.dp)
                } else {
                    Modifier.width(widthDp)
                }

                Box(
                    modifier = columnModifier.testTag("column_header_$column")
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
                                .testTag("resize_handle_$column")
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
                                            val newWidth = (startWidth + with(density) { accumulatedDrag.toDp() })
                                                .coerceIn(40.dp, 10000.dp)
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
    viewportWidth: Dp,
    showAnsiColors: Boolean = true,
    sourceIds: List<String> = emptyList(),
    missingSourceIds: Set<String> = emptySet(),
    columns: List<String>,
    columnWidths: Map<String, Int>,
    isSelected: Boolean = false,
    onClick: (Boolean, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val logColors = KLogViewerTheme.logColors
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary.copy(alpha = 0.15f)
    } else {
        getSourceBackgroundColor(entry.sourceId, sourceIds, isDarkMode)
    }

    Box(
        modifier = modifier
            .widthIn(min = viewportWidth)
            .background(backgroundColor)
            .semantics { selected = isSelected }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Release) {
                            val modifiers = event.keyboardModifiers
                            onClick(modifiers.isShiftPressed, modifiers.isMetaPressed || modifiers.isCtrlPressed)
                        }
                    }
                }
            }
            .clickable(
                onClick = {},
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Gutter / Line Number
        val gutterWidth = if (sourceIds.size > 1) 60.dp else 50.dp
        Row(
            modifier = Modifier.width(gutterWidth).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sourceIds.size > 1) {
                val isMissing = entry.sourceId != null && missingSourceIds.contains(entry.sourceId)
                val badgeColor = getSourceBadgeColor(entry.sourceId, sourceIds, isMissing)
                val tooltip = if (isMissing) "${entry.sourceId} (Missing)" else entry.sourceId ?: "Unknown Source"
                TooltipWrapper(tooltip = tooltip) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(badgeColor, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = lineNumber.toString().padStart(4, ' '),
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
                style = MaterialTheme.typography.caption,
                modifier = Modifier.weight(1f)
            )
        }
        
        columns.forEachIndexed { index, column ->
            val isLast = index == columns.lastIndex
            val widthDp = getColumnWidth(column, columnWidths)
            val isResized = columnWidths.containsKey(column)
            
            val columnModifier = if (isLast && !isResized) {
                val otherWidths = columns.dropLast(1).sumOf { getColumnWidth(it, columnWidths).value.toDouble() }.dp + gutterWidth
                val minWidth = if (viewportWidth - otherWidths > widthDp) viewportWidth - otherWidths else widthDp
                Modifier.widthIn(min = minWidth, max = 10000.dp)
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
                        entry.fields["level"] != null && entry.fields["level"] != "UNKNOWN" -> {
                            entry.fields["level"]!!
                        }
                        entry.level != LogLevel.UNKNOWN -> "[${entry.level}]"
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
                    val fullMessage = if (column == "Message") entry.content.value else entry.fields["content"] ?: entry.content.value
                    val displayMessage = if (fullMessage.length > 10000) fullMessage.take(10000) + "..." else fullMessage
                    val isMissing = entry.sourceId != null && missingSourceIds.contains(entry.sourceId)
                    Text(
                        text = LogHighlighter.highlight(displayMessage, filterQueries, isDarkMode, showAnsiColors),
                        style = MaterialTheme.typography.body1.copy(
                            textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None
                        ),
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

private fun getSourceBadgeColor(sourceId: String?, sourceIds: List<String>, isMissing: Boolean = false): Color {
    if (sourceId == null || sourceIds.size <= 1) return Color.Transparent
    if (isMissing) return Color.Red
    val index = sourceIds.indexOf(sourceId).coerceAtLeast(0)
    val colors = listOf(
        Color(0xFFE57373), // Red
        Color(0xFF81C784), // Green
        Color(0xFF64B5F6), // Blue
        Color(0xFFFFD54F), // Amber
        Color(0xFFBA68C8), // Purple
        Color(0xFF4DB6AC), // Teal
        Color(0xFFF06292), // Pink
        Color(0xFFAED581)  // Light Green
    )
    return colors[index % colors.size]
}

private fun getSourceBackgroundColor(sourceId: String?, sourceIds: List<String>, isDarkMode: Boolean): Color {
    if (sourceId == null || sourceIds.size <= 1) return Color.Transparent
    val index = sourceIds.indexOf(sourceId).coerceAtLeast(0)
    return if (isDarkMode) {
        val greys = listOf(
            Color(0xFF1E1E1E),
            Color(0xFF252525),
            Color(0xFF2D2D2D),
            Color(0xFF353535)
        )
        greys[index % greys.size]
    } else {
        val greys = listOf(
            Color(0xFFF9F9F9),
            Color(0xFFF2F2F2),
            Color(0xFFEBEBEB),
            Color(0xFFE4E4E4)
        )
        greys[index % greys.size]
    }
}

