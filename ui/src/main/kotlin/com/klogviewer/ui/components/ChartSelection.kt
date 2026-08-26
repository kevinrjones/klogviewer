package com.klogviewer.ui.components

import com.klogviewer.ui.mvi.DashboardTimeBucket
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

internal fun pointerXToBucketIndex(pointerX: Float, plotWidthPx: Float, bucketCount: Int): Int? {
    if (plotWidthPx <= 0f || bucketCount <= 0) return null
    return if (bucketCount == 1) 0 else {
        val clampedX = pointerX.coerceIn(0f, plotWidthPx)
        val normalizedX = clampedX / plotWidthPx
        (normalizedX * (bucketCount - 1)).roundToInt().coerceIn(0, bucketCount - 1)
    }
}

internal fun bucketRangeFromDrag(
    dragStartX: Float, dragEndX: Float, plotWidthPx: Float, bucketCount: Int
): IntRange? {
    val startIndex = pointerXToBucketIndex(dragStartX, plotWidthPx, bucketCount)
    val endIndex = pointerXToBucketIndex(dragEndX, plotWidthPx, bucketCount)
    return if (startIndex != null && endIndex != null) {
        minOf(startIndex, endIndex)..maxOf(startIndex, endIndex)
    } else {
        null
    }
}

internal enum class BucketSelectionVisualState {
    UNSELECTED, SELECTED, SELECTED_RANGE_ITEM
}

@Suppress("ReturnCount")
internal fun selectedBucketIndexRange(
    sortedBuckets: List<DashboardTimeBucket>,
    selectedBucketFrom: Instant?,
    selectedRangeFrom: Instant?,
    selectedRangeTo: Instant?
): IntRange? {
    val single = selectedBucketFrom?.let { from ->
        sortedBuckets.indexOfFirst { bucket -> bucket.from == from }
    }?.takeIf { it >= 0 }
    if (single != null) return single..single
    if (selectedRangeFrom == null || selectedRangeTo == null || selectedRangeFrom.isAfter(selectedRangeTo)) {
        return null
    }
    val indices = sortedBuckets.indices.filter { index ->
        val bucket = sortedBuckets[index]
        bucket.from >= selectedRangeFrom && bucket.to <= selectedRangeTo
    }
    val first = indices.firstOrNull()
    return if (first != null) first..indices.last() else null
}

internal fun bucketSelectionVisualState(index: Int, selectedRange: IntRange?): BucketSelectionVisualState {
    if (selectedRange == null || index !in selectedRange) return BucketSelectionVisualState.UNSELECTED
    return if (selectedRange.first == selectedRange.last) {
        BucketSelectionVisualState.SELECTED
    } else {
        BucketSelectionVisualState.SELECTED_RANGE_ITEM
    }
}

internal fun activeBucketSelectionRange(
    dragStartX: Float?, dragCurrentX: Float?, plotWidthPx: Float, bucketCount: Int
): IntRange? {
    return if (dragStartX != null && dragCurrentX != null) {
        bucketRangeFromDrag(dragStartX, dragCurrentX, plotWidthPx, bucketCount)
    } else {
        null
    }
}

internal fun timeBucketSelectionDescription(
    bucket: DashboardTimeBucket, formatter: DateTimeFormatter, visualState: BucketSelectionVisualState
): String {
    val selectionLabel = when (visualState) {
        BucketSelectionVisualState.UNSELECTED -> "not selected"
        BucketSelectionVisualState.SELECTED -> "selected"
        BucketSelectionVisualState.SELECTED_RANGE_ITEM -> "selected range item"
    }
    val fromStr = formatter.format(bucket.from)
    val toStr = formatter.format(bucket.to)
    return "Time bucket $fromStr to $toStr, ${bucket.count} events, $selectionLabel"
}
