package com.klogviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
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

internal const val NO_SOURCE_SHADE_INDEX = -1
internal val SourceShadeIndexSemanticsKey = SemanticsPropertyKey<Int>("sourceShadeIndex")
internal var SemanticsPropertyReceiver.sourceShadeIndex by SourceShadeIndexSemanticsKey

private val sourceBackgroundLightShades = generateDarkerGrayShades(
    argb = 0xFFFAFAFA,
    count = 50,
    step = 1
)

private val sourceBackgroundDarkShades = listOf(
    Color(0xFF1E1E1E),
    Color(0xFF242424),
    Color(0xFF2A2A2A),
    Color(0xFF303030),
    Color(0xFF363636),
    Color(0xFF3C3C3C)
)

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
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()
    var contextMenuRowIndex by remember { mutableStateOf<Int?>(null) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var latestSecondaryClickInContainer by remember { mutableStateOf<Offset?>(null) }
    var logListCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    LaunchedEffect(logs.size) {
        if (isAutoScrollEnabled && logs.isNotEmpty()) {
            verticalScrollState.scrollToItem(logs.size - 1)
        }
    }

    val displayColumns = if (columns.isEmpty()) listOf("Timestamp", "Level", "Message") else columns
    val logFontStyle = createLogFontStyle(logFontFamily, logFontSizeSp)

    val gutterWidth = getColumnWidth("Line #", columnWidths, sourceIds)
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
                                    sourceIds = sourceIds,
                                    missingSourceIds = missingSourceIds,
                                    columns = displayColumns,
                                    columnWidths = columnWidths,
                                    logFontStyle = logFontStyle,
                                    isSelected = selectedIndices.contains(index),
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
    }
}

@Composable
fun LogListHeader(
    columns: List<String>,
    columnWidths: Map<String, Int>,
    contentWidth: Dp,
    gutterWidth: Dp,
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
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    ResizeHandle(
                        column = "Line #",
                        currentWidth = gutterWidth,
                        onColumnResize = onColumnResize,
                        testTag = "resize_handle_gutter"
                    )
                }
            }
            
            columns.forEach { column ->
                val widthDp = getColumnWidth(column, columnWidths)
                val columnModifier = Modifier.width(widthDp)

                val headerTag = "column_header_$column"
                val handleTag = "resize_handle_$column"

                Box(
                    modifier = columnModifier.testTag(headerTag)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = column,
                            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        
                        ResizeHandle(
                            column = column,
                            currentWidth = widthDp,
                            onColumnResize = onColumnResize,
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
    modifier: Modifier = Modifier
) {
    val logColors = KLogViewerTheme.logColors
    val rowSourceShadeIndex = getSourceShadeIndex(entry.sourceId, sourceIds)
    val backgroundColor = if (isSelected) {
        MaterialTheme.colors.primary.copy(alpha = 0.15f)
    } else {
        getSourceBackgroundColor(rowSourceShadeIndex, isDarkMode)
    }

    var rowCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

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
                    missingSourceIds = missingSourceIds,
                    logColors = logColors,
                    logFontStyle = logFontStyle
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
        verticalAlignment = Alignment.Top
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

@Composable
private fun LogEntryCell(
    column: String,
    entry: LogEntry,
    columnModifier: Modifier,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    showAnsiColors: Boolean,
    missingSourceIds: Set<String>,
    logColors: LogLevelColors,
    logFontStyle: TextStyle
) {
    when (column) {
        "Timestamp" -> {
            Text(
                text = entry.timestamp.value,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.caption.copy(
                    fontFamily = logFontStyle.fontFamily,
                    fontSize = logFontStyle.fontSize
                ),
                modifier = columnModifier.padding(horizontal = 4.dp)
            )
        }
        "Level" -> {
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
                    fontFamily = logFontStyle.fontFamily,
                    fontSize = logFontStyle.fontSize,
                    textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None
                ),
                modifier = columnModifier.padding(horizontal = 4.dp)
            )
        }
        else -> {
            val value = resolveCustomColumnValue(column, entry)
            Text(
                text = value,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.caption.copy(
                    fontFamily = logFontStyle.fontFamily,
                    fontSize = logFontStyle.fontSize
                ),
                modifier = columnModifier.padding(horizontal = 4.dp)
            )
        }
    }
}

internal fun resolveCustomColumnValue(column: String, entry: LogEntry): String {
    val fields = entry.compatibilityFields()
    val normalizedColumn = column.normalizedFieldLookupKey()
    val snakeCaseColumn = column.lowercase().replace(" ", "_")

    return fields[column]
        ?: fields[snakeCaseColumn]
        ?: fields.entries.firstOrNull { (key, _) -> key.equals(column, ignoreCase = true) }?.value
        ?: fields.entries.firstOrNull { (key, _) ->
            key.normalizedFieldLookupKey() == normalizedColumn
        }?.value
        ?: ""
}

private fun String.normalizedFieldLookupKey(): String {
    return lowercase().filter { it.isLetterOrDigit() }
}

internal fun createLogFontStyle(fontFamily: String, fontSizeSp: Int): TextStyle {
    return TextStyle(
        fontFamily = resolveMonospacedFontFamily(fontFamily),
        fontSize = fontSizeSp.coerceIn(8, 72).sp
    )
}

internal fun resolveMonospacedFontFamily(fontFamily: String): FontFamily {
    return when (fontFamily.lowercase()) {
        "monospaced", "monospace", "dialoginput" -> FontFamily.Monospace
        else -> FontFamily.Monospace
    }
}

internal const val MAX_DEFAULT_COLUMN_WIDTH = 300
internal const val DEFAULT_MESSAGE_COLUMN_WIDTH = 1200

private fun getLogListContentWidth(
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
        "Line #", "#" -> if (sourceIds.size > 1) 60.dp else 50.dp
        "Timestamp" -> 180.dp
        "Level" -> 80.dp
        "Message", "Content" -> DEFAULT_MESSAGE_COLUMN_WIDTH.dp
        else -> 120.dp
    }

    return when (column) {
        "Message", "Content" -> defaultWidth.coerceAtMost(MAX_DEFAULT_COLUMN_WIDTH.dp)
        else -> defaultWidth.coerceAtMost(MAX_DEFAULT_COLUMN_WIDTH.dp)
    }
}

private fun getLevelColor(level: LogLevel, colors: LogLevelColors): Color = when (level) {
    LogLevel.TRACE -> colors.trace
    LogLevel.DEBUG -> colors.debug
    LogLevel.INFO -> colors.info
    LogLevel.WARN -> colors.warn
    LogLevel.ERROR -> colors.error
    LogLevel.FATAL -> colors.fatal
    LogLevel.UNKNOWN -> colors.unknown
}

private fun buildSourceBadgeTooltip(sourceId: String?, isMissing: Boolean): String {
    val fileName = sourceId.extractSourceFileName()
    return if (isMissing) "$fileName (Missing)" else fileName
}

private fun String?.extractSourceFileName(): String {
    if (this.isNullOrBlank()) return "Unknown Source"
    val normalized = this.removeSuffix("/").removeSuffix("\\")
    val fileName = normalized.substringAfterLast('/').substringAfterLast('\\')
    return fileName.ifBlank { normalized.ifBlank { "Unknown Source" } }
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

internal fun getSourceShadeIndex(sourceId: String?, sourceIds: List<String>): Int {
    if (sourceId.isNullOrBlank() || sourceIds.size <= 1) {
        return NO_SOURCE_SHADE_INDEX
    }
    return stableSourceShadeIndex(sourceId, sourceBackgroundLightShades.size)
}

internal fun generateDarkerGrayShades(argb: Long, count: Int, step: Int): List<Color> {
    if (count <= 0) return emptyList()

    val alpha = ((argb ushr 24) and 0xFF).toInt()
    val red = ((argb ushr 16) and 0xFF).toInt()
    val green = ((argb ushr 8) and 0xFF).toInt()
    val blue = (argb and 0xFF).toInt()
    val baseGray = ((red + green + blue) / 3).coerceIn(0, 255)
    val safeStep = step.coerceAtLeast(0)

    return (0 until count).map { index ->
        val darkenedChannel = (baseGray.toLong() - index.toLong() * safeStep.toLong())
            .coerceIn(0L, 255L)
            .toInt()

        val shadeArgb = (alpha.toLong() shl 24) or
            (darkenedChannel.toLong() shl 16) or
            (darkenedChannel.toLong() shl 8) or
            darkenedChannel.toLong()

        Color(shadeArgb)
    }
}

private fun stableSourceShadeIndex(sourceId: String, paletteSize: Int): Int {
    return Math.floorMod(sourceId.hashCode(), paletteSize)
}

private fun getSourceBackgroundColor(sourceShadeIndex: Int, isDarkMode: Boolean): Color {
    if (sourceShadeIndex == NO_SOURCE_SHADE_INDEX) return Color.Transparent
    val shades = if (isDarkMode) sourceBackgroundDarkShades else sourceBackgroundLightShades
    return shades[sourceShadeIndex % shades.size]
}

