package com.klogviewer.ui.components

import com.klogviewer.ui.mvi.DashboardTimeBucket
import java.time.Instant
import kotlin.math.ceil

private const val MIN_SAFE_MILLIS = 1000L
private const val MIN_SAFE_SECONDS = 1L
private const val MILLIS_PER_SECOND = 1000.0

internal fun rebucketTimeSeriesForDisplay(
    sortedBuckets: List<DashboardTimeBucket>,
    displayBucketDurationSeconds: Long
): List<DashboardTimeBucket> {
    if (sortedBuckets.isEmpty()) return emptyList()
    val normalizedBuckets = sortedBuckets.sortedBy { bucket -> bucket.from }
    val bucketDuration = displayBucketDurationSeconds.coerceAtLeast(1L)
    val alignedStart = alignEpochSecondFloor(normalizedBuckets.first().from.epochSecond, bucketDuration)
    val alignedEnd = alignEpochSecondCeil(normalizedBuckets.last().to.epochSecond, bucketDuration)
        .let { if (it <= alignedStart) alignedStart + bucketDuration else it }
    val bucketCount = ((alignedEnd - alignedStart) / bucketDuration).coerceAtLeast(1L).toInt()
    val counts = IntArray(bucketCount)
    normalizedBuckets.forEach { bucket ->
        val idx = ((bucket.from.epochSecond - alignedStart) / bucketDuration)
            .coerceIn(0L, bucketCount.toLong() - 1L).toInt()
        counts[idx] += bucket.count
    }
    return counts.indices.map { index ->
        val start = Instant.ofEpochSecond(alignedStart + index.toLong() * bucketDuration)
        DashboardTimeBucket(from = start, to = start.plusSeconds(bucketDuration), count = counts[index])
    }
}

internal fun timeSeriesSpanSeconds(sortedBuckets: List<DashboardTimeBucket>): Long {
    if (sortedBuckets.isEmpty()) return MIN_SAFE_SECONDS
    val minStart = sortedBuckets.minOf { it.from.toEpochMilli() }
    val maxEnd = sortedBuckets.maxOf { it.to.toEpochMilli() }
    val safeSpan = (maxEnd - minStart).coerceAtLeast(MIN_SAFE_MILLIS) / MILLIS_PER_SECOND
    return ceil(safeSpan).toLong().coerceAtLeast(MIN_SAFE_SECONDS)
}

internal fun bucketDurationSeconds(bucket: DashboardTimeBucket): Long {
    val millis = (bucket.to.toEpochMilli() - bucket.from.toEpochMilli()).coerceAtLeast(MIN_SAFE_MILLIS)
    return ceil(millis / MILLIS_PER_SECOND).toLong().coerceAtLeast(MIN_SAFE_SECONDS)
}

internal fun timeSeriesXAxisValues(sortedBuckets: List<DashboardTimeBucket>): List<Float> {
    return sortedBuckets.indices.map { it.toFloat() }
}

internal fun timeSeriesXAxisRange(xValues: List<Float>): ClosedFloatingPointRange<Float> {
    if (xValues.isEmpty()) return 0f..1f
    val minX = xValues.minOrNull() ?: 0f
    val maxX = xValues.maxOrNull() ?: 0f
    val minStep = xValues.distinct().sorted()
        .zipWithNext { a, b -> b - a }
        .filter { it > 0f }.minOrNull() ?: 1f
    val sidePadding = (minStep / 2f).coerceAtLeast(0.5f)
    return (minX - sidePadding)..(maxX + sidePadding)
}

private fun alignEpochSecondFloor(epochSecond: Long, stepSeconds: Long): Long {
    if (stepSeconds <= 0L) return epochSecond
    val remainder = epochSecond % stepSeconds
    return if (remainder >= 0L) epochSecond - remainder else epochSecond - (remainder + stepSeconds)
}

private fun alignEpochSecondCeil(epochSecond: Long, stepSeconds: Long): Long {
    val alignedFloor = alignEpochSecondFloor(epochSecond, stepSeconds)
    return if (alignedFloor == epochSecond) epochSecond else alignedFloor + stepSeconds
}
