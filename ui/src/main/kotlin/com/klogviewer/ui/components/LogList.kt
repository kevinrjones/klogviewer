package com.klogviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.theme.KLogViewerTheme
import com.klogviewer.ui.theme.LogLevelColors
import kotlin.math.roundToInt

private val COMPACT_MENU_ITEM_HEIGHT = 30.dp
private val COMPACT_MENU_ITEM_HORIZONTAL_PADDING = 10.dp

@Composable
private fun CompactMenuItem(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled

    Box(
        modifier = modifier
            .widthIn(min = 112.dp)
            .height(COMPACT_MENU_ITEM_HEIGHT)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = COMPACT_MENU_ITEM_HORIZONTAL_PADDING),
        contentAlignment = Alignment.CenterStart
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            Text(text = text, style = MaterialTheme.typography.body2)
        }
    }
}

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
    logFontFamily: String = "Monospaced",
    logFontSizeSp: Int = 12,
    selectedIndices: Set<Int> = emptySet(),
    onEntryClick: (LogEntry) -> Unit = {},
    onToggleSelection: (Int, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onContextCopy: () -> Unit = {},
    onContextRefresh: () -> Unit = {},
    onContextClear: () -> Unit = {},
    isContextCopyEnabled: Boolean = false,
    isContextRefreshEnabled: Boolean = true,
    isContextClearEnabled: Boolean = false,
    onColumnResize: (String, Int) -> Unit = { _, _ -> },
    windowId: String? = null,
    useCompactCellMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()
    var contextMenuRowIndex by remember { mutableStateOf<Int?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var latestSecondaryClickInContainer by remember { mutableStateOf<Offset?>(null) }
    var logListCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Popup state for cell value reveal
    var popupCellValue by remember { mutableStateOf<String?>(null) }
    var popupCellColumn by remember { mutableStateOf<String?>(null) }

    val onCellValueClick: (String, LogEntry) -> Unit = { column, entry ->
        val value = resolveCellValue(column, entry)
        if (value.isNotEmpty()) {
            popupCellValue = value
            popupCellColumn = column
        }
    }

    LaunchedEffect(logs.size) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            verticalScrollState.scrollToItem(logs.size - 1)
        }
    }

    val effectiveSourceIds = remember(sourceIds, logs) {
        getEffectiveSourceIds(sourceIds, logs)
    }

    val displayColumns = if (columns.isEmpty()) listOf("Timestamp", "Level", "Message") else columns
    val logFontStyle = createLogFontStyle(logFontFamily, logFontSizeSp)

    val gutterWidth = getColumnWidth("Line #", columnWidths, effectiveSourceIds)
    val contentWidth = getLogListContentWidth(displayColumns, columnWidths, gutterWidth)
    val logListTag = if (windowId != null) "log_list_$windowId" else "log_list"

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(logListTag)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            latestSecondaryClickInContainer = event.changes.firstOrNull()?.position
                        }
                    }
                }
            }
            .onGloballyPositioned { logListCoordinates = it }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("log_horizontal_scroll_container")
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LogListHeader(
                        columns = displayColumns,
                        columnWidths = columnWidths,
                        contentWidth = contentWidth,
                        gutterWidth = gutterWidth,
                        onColumnResize = onColumnResize,
                        logs = logs,
                        sourceIds = effectiveSourceIds,
                        logFontSizeSp = logFontSizeSp,
                        modifier = Modifier.width(contentWidth)
                    )
                    LazyColumn(
                        state = verticalScrollState,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(contentWidth)
                            .testTag("log_lazy_column")
                    ) {
                        itemsIndexed(logs) { index, entry ->
                            Box(modifier = Modifier.testTag("log_entry_row")) {
                                LogEntryRow(
                                    entry = entry,
                                    lineNumber = index + 1,
                                    filterQueries = filterQueries,
                                    isDarkMode = isDarkMode,
                                    contentWidth = contentWidth,
                                    gutterWidth = gutterWidth,
                                    showAnsiColors = showAnsiColors,
                                    sourceIds = effectiveSourceIds,
                                    missingSourceIds = missingSourceIds,
                                    columns = displayColumns,
                                    columnWidths = columnWidths,
                                    logFontStyle = logFontStyle,
                                    isSelected = selectedIndices.contains(index),
                                    useCompactCellMode = useCompactCellMode,
                                    onCellValueClick = onCellValueClick,
                                    onClick = { isShift, isMeta ->
                                        contextMenuRowIndex = null
                                        if (isShift || isMeta) {
                                            onToggleSelection(index, isShift, isMeta)
                                        } else {
                                            onEntryClick(entry)
                                        }
                                    },
                                    menuContainerCoordinates = logListCoordinates,
                                    onContextMenuRequested = { clickOffset ->
                                        contextMenuRowIndex = index
                                        contextMenuOffset = latestSecondaryClickInContainer ?: clickOffset
                                    },
                                    modifier = Modifier.testTag("log_entry_row_$index")
                                )
                            }
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

        if (contextMenuRowIndex != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = contextMenuOffset.x.roundToInt(),
                            y = contextMenuOffset.y.roundToInt()
                        )
                    }
                    .size(1.dp)
            ) {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = { contextMenuRowIndex = null },
                    offset = DpOffset(0.dp, 0.dp),
                    modifier = Modifier.testTag("log_context_menu")
                ) {
                    CompactMenuItem(
                        text = "Copy",
                        onClick = {
                            contextMenuRowIndex = null
                            onContextCopy()
                        },
                        enabled = isContextCopyEnabled,
                        modifier = Modifier.testTag("log_context_menu_copy")
                    )
                    CompactMenuItem(
                        text = "Refresh",
                        onClick = {
                            contextMenuRowIndex = null
                            onContextRefresh()
                        },
                        enabled = isContextRefreshEnabled,
                        modifier = Modifier.testTag("log_context_menu_refresh")
                    )
                    CompactMenuItem(
                        text = "Clear",
                        onClick = {
                            contextMenuRowIndex = null
                            onContextClear()
                        },
                        enabled = isContextClearEnabled,
                        modifier = Modifier.testTag("log_context_menu_clear")
                    )
                }
            }
        }

        // Cell value popup
        CellValuePopupState(
            popupCellValue = popupCellValue,
            popupCellColumn = popupCellColumn,
            onDismiss = {
                popupCellValue = null
                popupCellColumn = null
            }
        )
    }
}

@Composable
private fun CellValuePopupState(
    popupCellValue: String?,
    popupCellColumn: String?,
    onDismiss: () -> Unit
) {
    val cellValue = popupCellValue
    val cellColumn = popupCellColumn
    if (cellValue != null && cellColumn != null) {
        CellValuePopup(
            value = cellValue,
            columnName = cellColumn,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun LogListHeader(
    columns: List<String>,
    columnWidths: Map<String, Int>,
    contentWidth: Dp,
    gutterWidth: Dp,
    onColumnResize: (String, Int) -> Unit,
    logs: List<LogEntry> = emptyList(),
    sourceIds: List<String> = emptyList(),
    logFontSizeSp: Int = 12,
    modifier: Modifier = Modifier
) {
    Surface(
        color = KLogViewerTheme.customColors.toolbarSurface,
        elevation = 1.dp,
        modifier = modifier.height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .width(contentWidth)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(gutterWidth).testTag("column_header_gutter")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    val onAutoResizeGutter = remember(logs, sourceIds, logFontSizeSp) {
                        {
                            val autoWidth = calculateColumnWidthToContent(
                                column = "Line #",
                                logs = logs,
                                sourceIds = sourceIds,
                                logFontSizeSp = logFontSizeSp
                            )
                            onColumnResize("Line #", autoWidth)
                        }
                    }
                    ResizeHandle(
                        column = "Line #",
                        currentWidth = gutterWidth,
                        onColumnResize = onColumnResize,
                        onAutoResize = onAutoResizeGutter,
                        testTag = "resize_handle_gutter"
                    )
                }
            }
            
            columns.forEach { column ->
                val widthDp = getColumnWidth(column, columnWidths)
                val columnModifier = Modifier.width(widthDp)

                val headerTag = "column_header_$column"
                val handleTag = "resize_handle_$column"

                val onAutoResizeColumn = remember(column, logs, sourceIds, logFontSizeSp) {
                    {
                        val autoWidth = calculateColumnWidthToContent(
                            column = column,
                            logs = logs,
                            sourceIds = sourceIds,
                            logFontSizeSp = logFontSizeSp
                        )
                        onColumnResize(column, autoWidth)
                    }
                }

                Box(
                    modifier = columnModifier.testTag(headerTag)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.overline.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        
                        ResizeHandle(
                            column = column,
                            currentWidth = widthDp,
                            onColumnResize = onColumnResize,
                            onAutoResize = onAutoResizeColumn,
                            testTag = handleTag
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(
    column: String,
    currentWidth: Dp,
    onColumnResize: (String, Int) -> Unit,
    onAutoResize: (() -> Unit)? = null,
    testTag: String? = null
) {
    val density = LocalDensity.current
    val latestWidth by rememberUpdatedState(currentWidth)
    var dragWidth by remember(column) { mutableStateOf(currentWidth) }

    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight()
            .testTag(testTag ?: "resize_handle_$column")
            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.E_RESIZE_CURSOR)))
            .pointerInput(column, onAutoResize) {
                if (onAutoResize != null) {
                    detectTapGestures(
                        onDoubleTap = {
                            onAutoResize()
                        }
                    )
                }
            }
            .pointerInput(column) {
                detectDragGestures(
                    onDragStart = {
                        dragWidth = latestWidth
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()

                        val deltaDp = with(density) { dragAmount.x.toDp() }
                        val newWidth = (dragWidth + deltaDp).coerceIn(40.dp, 10000.dp)

                        if (newWidth != dragWidth) {
                            dragWidth = newWidth
                            onColumnResize(column, newWidth.value.roundToInt())
                        }
                    },
                    onDragCancel = {
                        dragWidth = latestWidth
                    },
                    onDragEnd = {
                        dragWidth = latestWidth
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight(0.6f)
                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun LogEntryRow(
    entry: LogEntry,
    lineNumber: Int,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    contentWidth: Dp,
    gutterWidth: Dp,
    showAnsiColors: Boolean = true,
    sourceIds: List<String> = emptyList(),
    missingSourceIds: Set<String> = emptySet(),
    columns: List<String>,
    columnWidths: Map<String, Int>,
    logFontStyle: TextStyle,
    isSelected: Boolean = false,
    onClick: (Boolean, Boolean) -> Unit = { _, _ -> },
    menuContainerCoordinates: LayoutCoordinates? = null,
    onContextMenuRequested: (Offset) -> Unit = {},
    useCompactCellMode: Boolean = true,
    onCellValueClick: (String, LogEntry) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val logColors = KLogViewerTheme.logColors
    val rowSourceShadeIndex = getSourceShadeIndex(entry.sourceId, sourceIds)
    val backgroundColor = if (isSelected) {
        KLogViewerTheme.customColors.selectedRow
    } else {
        getSourceBackgroundColor(rowSourceShadeIndex, isDarkMode)
    }

    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val gridLineColor = if (isDarkMode) {
        MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.18f)
    }

    Box(
        modifier = modifier
            .width(contentWidth)
            .background(backgroundColor)
            .semantics {
                selected = isSelected
                sourceShadeIndex = rowSourceShadeIndex
            }
            .onGloballyPositioned { rowCoordinates = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var suppressNextReleaseClick = false
                    var lastKnownPointerPosition: Offset? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val eventPosition = event.changes.firstOrNull()?.position
                        val previousEventPosition = event.changes.firstOrNull()?.previousPosition
                        if (event.type != PointerEventType.Press && eventPosition != null) {
                            lastKnownPointerPosition = eventPosition
                        }

                        when {
                            event.type == PointerEventType.Press && event.buttons.isSecondaryPressed -> {
                                suppressNextReleaseClick = true
                                val clickOffset = eventPosition
                                    ?: previousEventPosition
                                    ?: lastKnownPointerPosition
                                    ?: Offset.Zero
                                val containerCoordinates = menuContainerCoordinates
                                val currentRowCoordinates = rowCoordinates
                                val menuPosition = if (containerCoordinates != null && currentRowCoordinates != null) {
                                    val rowPositionInRoot = currentRowCoordinates.positionInRoot()
                                    val containerPositionInRoot = containerCoordinates.positionInRoot()
                                    Offset(
                                        x = rowPositionInRoot.x + clickOffset.x - containerPositionInRoot.x,
                                        y = rowPositionInRoot.y + clickOffset.y - containerPositionInRoot.y
                                    )
                                } else {
                                    clickOffset
                                }
                                onContextMenuRequested(menuPosition)
                                event.changes.forEach { it.consume() }
                                if (eventPosition != null) {
                                    lastKnownPointerPosition = eventPosition
                                }
                            }
                            event.type == PointerEventType.Release && suppressNextReleaseClick -> {
                                suppressNextReleaseClick = false
                            }
                            event.type == PointerEventType.Release -> {
                                val modifiers = event.keyboardModifiers
                                onClick(modifiers.isShiftPressed, modifiers.isMetaPressed || modifiers.isCtrlPressed)
                            }
                        }
                    }
                }
            }
            .clickable(
                onClick = {},
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            )
            .drawBehind {
                val strokeWidth = 1f

                // Horizontal line at the bottom of the row
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, size.height - strokeWidth),
                    end = Offset(size.width, size.height - strokeWidth),
                    strokeWidth = strokeWidth
                )

                // Vertical line after the gutter
                val gutterWidthPx = gutterWidth.toPx()
                drawLine(
                    color = gridLineColor,
                    start = Offset(gutterWidthPx, 0f),
                    end = Offset(gutterWidthPx, size.height),
                    strokeWidth = strokeWidth
                )

                // Vertical lines at column boundaries (including the last column's right edge)
                var cumulativeX = gutterWidthPx
                columns.forEach { column ->
                    val colWidthDp = getColumnWidth(column, columnWidths)
                    cumulativeX += colWidthDp.toPx()
                    if (cumulativeX <= size.width) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(cumulativeX, 0f),
                            end = Offset(cumulativeX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.Top
        ) {
            LogGutter(
                entry = entry,
                lineNumber = lineNumber,
                sourceIds = sourceIds,
                missingSourceIds = missingSourceIds,
                gutterWidth = gutterWidth,
                logFontStyle = logFontStyle
            )
            
            columns.forEach { column ->
                val widthDp = getColumnWidth(column, columnWidths)
                val columnModifier = Modifier.width(widthDp)

                LogEntryCell(
                    column = column,
                    entry = entry,
                    columnModifier = columnModifier,
                    filterQueries = filterQueries,
                    isDarkMode = isDarkMode,
                    showAnsiColors = showAnsiColors,
                    sourceIds = sourceIds,
                    missingSourceIds = missingSourceIds,
                    logColors = logColors,
                    logFontStyle = logFontStyle,
                    useCompactCellMode = useCompactCellMode,
                    onCellValueClick = onCellValueClick
                )
            }
        }
        
    }
}

@Composable
private fun LogGutter(
    entry: LogEntry,
    lineNumber: Int,
    sourceIds: List<String>,
    missingSourceIds: Set<String>,
    gutterWidth: Dp,
    logFontStyle: TextStyle
) {
    val rowIndex = lineNumber - 1
    Row(
        modifier = Modifier.width(gutterWidth).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (sourceIds.size > 1) {
            val isMissing = entry.sourceId != null && missingSourceIds.contains(entry.sourceId)
            val badgeColor = getSourceBadgeColor(entry.sourceId, sourceIds, isMissing)
            val tooltip = buildSourceBadgeTooltip(entry.sourceId, isMissing)
            TooltipWrapper(
                tooltip = tooltip,
                tooltipTestTag = "log_source_badge_tooltip_$rowIndex"
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(8.dp)
                        .testTag("log_source_badge_$rowIndex")
                        .background(badgeColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (entry.structuredData != null) {
            TooltipWrapper(
                tooltip = "Structured payload available",
                tooltipTestTag = "log_structured_badge_tooltip_$rowIndex"
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(8.dp)
                        .testTag("log_structured_badge_$rowIndex")
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.7f), CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = lineNumber.toString().padStart(4, ' '),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            style = MaterialTheme.typography.caption.copy(
                fontFamily = logFontStyle.fontFamily,
                fontSize = logFontStyle.fontSize
            ),
            modifier = Modifier.weight(1f)
        )
    }
}


