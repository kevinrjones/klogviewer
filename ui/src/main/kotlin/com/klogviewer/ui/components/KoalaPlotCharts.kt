package com.klogviewer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.mvi.DashboardBucketSize
import com.klogviewer.ui.mvi.DashboardLevelSlice
import com.klogviewer.ui.mvi.DashboardTimeBucket
import com.klogviewer.ui.theme.KLogViewerTheme
import com.klogviewer.ui.theme.LogLevelColors
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.bar.VerticalBarPlotEntry
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.AxisStyle
import io.github.koalaplot.core.xygraph.GridStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

internal fun timeAxisLabelFormatter(
    bucketSize: DashboardBucketSize,
    zoneId: ZoneId = ZoneId.systemDefault()
): DateTimeFormatter {
    val pattern = when (bucketSize) {
        DashboardBucketSize.PER_SECOND -> "HH:mm:ss"
        DashboardBucketSize.PER_MINUTE -> "HH:mm"
    }
    return DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
}

internal fun timeAxisDateTooltipFormatter(zoneId: ZoneId = ZoneId.systemDefault()): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)
}

internal fun orderedLevelDistributionSlices(slices: List<DashboardLevelSlice>): List<DashboardLevelSlice> {
    return slices.sortedBy { slice ->
        when (slice.level) {
            LogLevel.DEBUG -> 0
            LogLevel.INFO -> 1
            LogLevel.WARN -> 2
            LogLevel.ERROR -> 3
            LogLevel.FATAL -> 4
            LogLevel.UNKNOWN -> 5
        }
    }
}

internal fun formatLevelDistributionPercentage(ratio: Float): String {
    val normalizedRatio = ratio.coerceIn(0f, 1f)
    if (normalizedRatio == 0f) {
        return "0%"
    }

    val percentage = normalizedRatio * 100f
    return when {
        percentage < 0.1f -> "<0.1%"
        percentage < 10f -> String.format(Locale.US, "%.1f%%", percentage)
        else -> "${percentage.roundToInt()}%"
    }
}

internal fun normalizedPieValues(slices: List<DashboardLevelSlice>): List<Float> {
    val nonNegativeRatios = slices.map { slice -> slice.ratio.coerceAtLeast(0f) }
    val ratioSum = nonNegativeRatios.sum()
    if (ratioSum <= 0f) {
        return nonNegativeRatios
    }

    return nonNegativeRatios.map { ratio -> ratio / ratioSum }
}

internal fun timeSeriesXAxisValues(sortedBuckets: List<DashboardTimeBucket>): List<Float> {
    if (sortedBuckets.isEmpty()) {
        return emptyList()
    }

    val axisUnitSeconds = timeSeriesAxisUnitSeconds(sortedBuckets)
    val axisOriginMillis = sortedBuckets.first().from.toEpochMilli()
    return sortedBuckets.map { bucket ->
        val elapsedMillis = bucket.from.toEpochMilli() - axisOriginMillis
        val elapsedSeconds = (elapsedMillis / 1000.0).toFloat()
        elapsedSeconds / axisUnitSeconds
    }
}

internal fun timeSeriesAxisUnitSeconds(sortedBuckets: List<DashboardTimeBucket>): Float {
    if (sortedBuckets.isEmpty()) {
        return 1f
    }

    return sortedBuckets
        .map { bucket ->
            val durationMillis = (bucket.to.toEpochMilli() - bucket.from.toEpochMilli()).coerceAtLeast(1000L)
            (durationMillis / 1000.0).toFloat()
        }
        .minOrNull()
        ?.coerceAtLeast(1f)
        ?: 1f
}

internal fun timeSeriesXAxisRange(xValues: List<Float>): ClosedFloatingPointRange<Float> {
    if (xValues.isEmpty()) {
        return 0f..1f
    }

    val minX = xValues.minOrNull() ?: 0f
    val maxX = xValues.maxOrNull() ?: 0f
    if (minX == maxX) {
        return minX..(minX + 1f)
    }

    return minX..maxX
}

internal fun timeSeriesAxisInstant(axisOrigin: Instant, axisValue: Float, axisUnitSeconds: Float): Instant {
    val elapsedMillis = (axisValue * axisUnitSeconds * 1000f).roundToLong().coerceAtLeast(0L)
    return axisOrigin.plusMillis(elapsedMillis)
}

internal fun pointerXToBucketIndex(pointerX: Float, plotWidthPx: Float, bucketCount: Int): Int? {
    if (plotWidthPx <= 0f || bucketCount <= 0) {
        return null
    }

    if (bucketCount == 1) {
        return 0
    }

    val clampedX = pointerX.coerceIn(0f, plotWidthPx)
    val normalizedX = clampedX / plotWidthPx
    val scaledIndex = normalizedX * (bucketCount - 1)
    return scaledIndex.roundToInt().coerceIn(0, bucketCount - 1)
}

internal fun bucketRangeFromDrag(
    dragStartX: Float,
    dragEndX: Float,
    plotWidthPx: Float,
    bucketCount: Int
): IntRange? {
    val startIndex = pointerXToBucketIndex(dragStartX, plotWidthPx, bucketCount) ?: return null
    val endIndex = pointerXToBucketIndex(dragEndX, plotWidthPx, bucketCount) ?: return null
    return minOf(startIndex, endIndex)..maxOf(startIndex, endIndex)
}

internal enum class BucketSelectionVisualState {
    UNSELECTED,
    SELECTED,
    SELECTED_RANGE_ITEM
}

internal fun selectedBucketIndexRange(
    sortedBuckets: List<DashboardTimeBucket>,
    selectedBucketFrom: Instant?,
    selectedRangeFrom: Instant?,
    selectedRangeTo: Instant?
): IntRange? {
    if (sortedBuckets.isEmpty()) {
        return null
    }

    val selectedSingleIndex = selectedBucketFrom
        ?.let { selectedFrom -> sortedBuckets.indexOfFirst { bucket -> bucket.from == selectedFrom } }
        ?.takeIf { index -> index >= 0 }

    if (selectedSingleIndex != null) {
        return selectedSingleIndex..selectedSingleIndex
    }

    if (selectedRangeFrom == null || selectedRangeTo == null || selectedRangeFrom.isAfter(selectedRangeTo)) {
        return null
    }

    val selectedIndices = sortedBuckets.indices
        .filter { index ->
            val bucket = sortedBuckets[index]
            bucket.from >= selectedRangeFrom && bucket.to <= selectedRangeTo
        }

    if (selectedIndices.isEmpty()) {
        return null
    }

    return selectedIndices.first()..selectedIndices.last()
}

internal fun bucketSelectionVisualState(index: Int, selectedRange: IntRange?): BucketSelectionVisualState {
    if (selectedRange == null || index !in selectedRange) {
        return BucketSelectionVisualState.UNSELECTED
    }

    return if (selectedRange.first == selectedRange.last) {
        BucketSelectionVisualState.SELECTED
    } else {
        BucketSelectionVisualState.SELECTED_RANGE_ITEM
    }
}

internal fun activeBucketSelectionRange(
    dragStartX: Float?,
    dragCurrentX: Float?,
    plotWidthPx: Float,
    bucketCount: Int
): IntRange? {
    val dragPreviewRange = if (dragStartX != null && dragCurrentX != null) {
        bucketRangeFromDrag(
            dragStartX = dragStartX,
            dragEndX = dragCurrentX,
            plotWidthPx = plotWidthPx,
            bucketCount = bucketCount
        )
    } else {
        null
    }

    return dragPreviewRange
}

internal fun timeBucketSelectionDescription(
    bucket: DashboardTimeBucket,
    formatter: DateTimeFormatter,
    visualState: BucketSelectionVisualState
): String {
    val selectionLabel = when (visualState) {
        BucketSelectionVisualState.UNSELECTED -> "not selected"
        BucketSelectionVisualState.SELECTED -> "selected"
        BucketSelectionVisualState.SELECTED_RANGE_ITEM -> "selected range item"
    }
    return "Time bucket ${formatter.format(bucket.from)} to ${formatter.format(bucket.to)}, ${bucket.count} events, $selectionLabel"
}

private fun Modifier.drawRangeOverlay(
    range: IntRange?,
    bucketCount: Int,
    color: Color
): Modifier {
    if (range == null || range.first == range.last || bucketCount <= 0) {
        return this
    }

    return drawBehind {
        val slotWidth = size.width / bucketCount
        val startX = slotWidth * range.first
        val endX = slotWidth * (range.last + 1)
        drawRect(
            color = color,
            topLeft = Offset(startX, 0f),
            size = Size(width = (endX - startX).coerceAtLeast(0f), height = size.height)
        )
    }
}

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalFoundationApi::class)
@Composable
fun KoalaPlotTimeSeriesChart(
    buckets: List<DashboardTimeBucket>,
    bucketSize: DashboardBucketSize,
    selectedBucketFrom: Instant?,
    selectedRangeFrom: Instant? = null,
    selectedRangeTo: Instant? = null,
    onBucketSelect: (DashboardTimeBucket) -> Unit,
    onBucketRangeSelect: (DashboardTimeBucket, DashboardTimeBucket) -> Unit = { fromBucket, _ ->
        onBucketSelect(fromBucket)
    },
    modifier: Modifier = Modifier,
    chartHeight: Dp = 180.dp
) {
    if (buckets.isEmpty()) {
        Box(modifier = modifier.height(chartHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.caption)
        }
        return
    }

    val timeFormatter = remember(bucketSize) { timeAxisLabelFormatter(bucketSize) }
    val dateTooltipFormatter = remember { timeAxisDateTooltipFormatter() }

    val sortedBuckets = remember(buckets) {
        buckets.sortedBy { bucket -> bucket.from }
    }

    val xValues = remember(sortedBuckets) { timeSeriesXAxisValues(sortedBuckets) }
    val yValues = remember(sortedBuckets) { sortedBuckets.map { bucket -> bucket.count.toFloat() } }
    val maxCount = remember(yValues) { yValues.maxOrNull() ?: 0f }
    val xAxisRange = remember(xValues) { timeSeriesXAxisRange(xValues) }
    val axisOrigin = remember(sortedBuckets) { sortedBuckets.first().from }
    val axisUnitSeconds = remember(sortedBuckets) { timeSeriesAxisUnitSeconds(sortedBuckets) }

    val xAxisModel = remember(xAxisRange) {
        FloatLinearAxisModel(
            range = xAxisRange,
            minimumMajorTickSpacing = 80.dp
        )
    }
    val yAxisModel = remember(maxCount) {
        FloatLinearAxisModel(
            range = 0f..maxCount.coerceAtLeast(1f),
            minimumMajorTickSpacing = 30.dp
        )
    }

    var chartWidthPx by remember { mutableStateOf(0f) }
    var dragStartX by remember { mutableStateOf<Float?>(null) }
    var dragCurrentX by remember { mutableStateOf<Float?>(null) }

    val activeSelectionRange = remember(
        dragStartX,
        dragCurrentX,
        chartWidthPx,
        sortedBuckets.size
    ) {
        activeBucketSelectionRange(
            dragStartX = dragStartX,
            dragCurrentX = dragCurrentX,
            plotWidthPx = chartWidthPx,
            bucketCount = sortedBuckets.size
        )
    }

    val chartModifier = modifier
        .height(chartHeight)
        .fillMaxWidth()
        .padding(8.dp)
        .drawRangeOverlay(
            range = activeSelectionRange,
            bucketCount = sortedBuckets.size,
            color = MaterialTheme.colors.primary.copy(alpha = 0.08f)
        )
        .onSizeChanged { size ->
            chartWidthPx = size.width.toFloat()
        }
        .pointerInput(sortedBuckets, chartWidthPx) {
            detectDragGestures(
                onDragStart = { offset ->
                    dragStartX = offset.x
                    dragCurrentX = offset.x
                },
                onDrag = { change, _ ->
                    dragCurrentX = change.position.x
                    change.consume()
                },
                onDragEnd = {
                    val startX = dragStartX
                    val endX = dragCurrentX

                    if (startX != null && endX != null) {
                        val selectedRange = bucketRangeFromDrag(
                            dragStartX = startX,
                            dragEndX = endX,
                            plotWidthPx = chartWidthPx,
                            bucketCount = sortedBuckets.size
                        )

                        selectedRange?.let { range ->
                            val fromBucket = sortedBuckets[range.first]
                            val toBucket = sortedBuckets[range.last]
                            if (range.first == range.last) {
                                onBucketSelect(fromBucket)
                            } else {
                                onBucketRangeSelect(fromBucket, toBucket)
                            }
                        }
                    }

                    dragStartX = null
                    dragCurrentX = null
                },
                onDragCancel = {
                    dragStartX = null
                    dragCurrentX = null
                }
            )
        }

    XYGraph(
        xAxisModel = xAxisModel,
        yAxisModel = yAxisModel,
        modifier = chartModifier,
        xAxisContent = AxisContent(
            style = AxisStyle(),
            title = {
                Text("Time", style = MaterialTheme.typography.caption)
            },
            labels = { value: Float ->
                val axisInstant = timeSeriesAxisInstant(
                    axisOrigin = axisOrigin,
                    axisValue = value,
                    axisUnitSeconds = axisUnitSeconds
                )
                TooltipArea(
                    tooltip = {
                        Surface(
                            modifier = Modifier.shadow(4.dp),
                            color = MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = dateTooltipFormatter.format(axisInstant),
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    },
                    delayMillis = 500,
                    tooltipPlacement = TooltipPlacement.CursorPoint(
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(0.dp, 16.dp)
                    )
                ) {
                    Text(
                        text = timeFormatter.format(axisInstant),
                        style = MaterialTheme.typography.caption
                    )
                    }
            }
        ),
        yAxisContent = AxisContent(
            title = {
                Text(
                    "Count",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.graphicsLayer { rotationZ = -90f }
                )
            },
            labels = { value: Float ->
                if (value == value.toInt().toFloat()) {
                    Text("${value.toInt()}", style = MaterialTheme.typography.caption)
                }
            },
            style = AxisStyle()
        ),
        gridStyle = GridStyle(
            horizontalMajorStyle = null,
            verticalMajorStyle = null,
            horizontalMinorStyle = null,
            verticalMinorStyle = null
        ),
    ) {
        VerticalBarPlot(
            xData = xValues,
            yData = yValues,
            barWidth = 0.9f,
            bar = { index: Int, _: Int, _: VerticalBarPlotEntry<Float, Float> ->
                val bucket = sortedBuckets[index]
                val visualState = bucketSelectionVisualState(index, activeSelectionRange)
                val fillColor = when (visualState) {
                    BucketSelectionVisualState.UNSELECTED -> MaterialTheme.colors.primary.copy(alpha = 0.55f)
                    BucketSelectionVisualState.SELECTED -> MaterialTheme.colors.primary
                    BucketSelectionVisualState.SELECTED_RANGE_ITEM -> MaterialTheme.colors.primary.copy(alpha = 0.9f)
                }
                val borderColor = when (visualState) {
                    BucketSelectionVisualState.UNSELECTED -> MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    BucketSelectionVisualState.SELECTED,
                    BucketSelectionVisualState.SELECTED_RANGE_ITEM -> MaterialTheme.colors.onSurface
                }
                val borderWidth = when (visualState) {
                    BucketSelectionVisualState.UNSELECTED -> 0.5.dp
                    BucketSelectionVisualState.SELECTED -> 2.dp
                    BucketSelectionVisualState.SELECTED_RANGE_ITEM -> 1.4.dp
                }
                val showTopMarker = visualState != BucketSelectionVisualState.UNSELECTED

                TooltipArea(
                    tooltip = {
                        Surface(
                            modifier = Modifier.shadow(4.dp),
                            color = MaterialTheme.colors.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Bucket start: ${timeFormatter.format(bucket.from)}",
                                    style = MaterialTheme.typography.caption
                                )
                                Text(
                                    text = "Bucket end: ${timeFormatter.format(bucket.to)}",
                                    style = MaterialTheme.typography.caption
                                )
                                Text(
                                    text = "Event count: ${bucket.count}",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    },
                    delayMillis = 500,
                    tooltipPlacement = TooltipPlacement.CursorPoint(
                        alignment = Alignment.BottomEnd,
                        offset = DpOffset(0.dp, 16.dp)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(fillColor)
                            .border(borderWidth, borderColor)
                            .pointerHoverIcon(PointerIcon(java.awt.Cursor(java.awt.Cursor.HAND_CURSOR)))
                            .semantics {
                                contentDescription = timeBucketSelectionDescription(
                                    bucket = bucket,
                                    formatter = timeFormatter,
                                    visualState = visualState
                                )
                            }
                            .clickable { onBucketSelect(bucket) }
                    ) {
                        if (showTopMarker) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (visualState == BucketSelectionVisualState.SELECTED) 3.dp else 2.dp)
                                    .background(MaterialTheme.colors.onPrimary.copy(alpha = 0.85f))
                                    .align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun KoalaPlotLevelDistributionChart(
    slices: List<DashboardLevelSlice>,
    selectedLevel: LogLevel?,
    onLevelSelect: (LogLevel) -> Unit,
    modifier: Modifier = Modifier,
    chartSize: Dp = 180.dp
) {
    val piePadding = 20.dp
    val orderedSlices = remember(slices) { orderedLevelDistributionSlices(slices) }
    val totalCount = orderedSlices.sumOf { it.count }
    val selectedSlice = orderedSlices.firstOrNull { it.level == selectedLevel }
    val visibleSlices = orderedSlices.filter { it.count > 0 }
    val normalizedValues = remember(visibleSlices) { normalizedPieValues(visibleSlices) }
    val pieDiameter = chartSize - (piePadding * 2)

    if (orderedSlices.isEmpty()) {
        Box(modifier = modifier.size(chartSize), contentAlignment = Alignment.Center) {
            Text("No level data", style = MaterialTheme.typography.caption)
        }
        return
    }

    val colors = KLogViewerTheme.logColors

    Box(
        modifier = modifier
            .size(chartSize)
            .semantics {
                val selectedSummary = selectedSlice?.let {
                    "Selected ${it.level.name}: ${it.count} (${formatLevelDistributionPercentage(it.ratio)})"
                }
                contentDescription = listOfNotNull(
                    "Level distribution donut chart",
                    "Total events: $totalCount",
                    "Detailed level values are listed below the chart",
                    selectedSummary
                ).joinToString(separator = ". ")
            },
        contentAlignment = Alignment.Center
    ) {
        if (totalCount > 0 && visibleSlices.isNotEmpty()) {
            PieChart(
                values = normalizedValues,
                modifier = Modifier
                    .matchParentSize()
                    .padding(piePadding),
                slice = { index ->
                    val level = visibleSlices[index].level
                    val color = levelDistributionColor(level = level, colors = colors)
                    val alpha = if (selectedLevel == null || selectedLevel == level) 1f else 0.35f
                    DefaultSlice(
                        color = color.copy(alpha = alpha),
                        clickable = true,
                        onClick = { onLevelSelect(level) }
                    )
                },
                label = {}
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.16f), CircleShape)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.04f), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(pieDiameter * 0.58f)
                .background(MaterialTheme.colors.surface, CircleShape)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = (selectedSlice?.count ?: totalCount).toString(),
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = selectedSlice?.level?.name ?: "events",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

private fun levelDistributionColor(level: LogLevel, colors: LogLevelColors): Color {
    return when (level) {
        LogLevel.DEBUG -> colors.debug
        LogLevel.INFO -> colors.info
        LogLevel.WARN -> colors.warn
        LogLevel.ERROR -> colors.error
        LogLevel.FATAL -> colors.fatal
        LogLevel.UNKNOWN -> colors.unknown
    }
}
