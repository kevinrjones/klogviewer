package com.klogviewer.ui.components

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.mvi.DashboardBucketSize
import com.klogviewer.ui.mvi.DashboardLevelSlice
import com.klogviewer.ui.mvi.DashboardTimeBucket
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

internal const val MIN_PIXELS_PER_DISPLAY_BUCKET = 8f
internal const val DEFAULT_TIME_SERIES_CHART_WIDTH_PX = 1200f

internal val NICE_DISPLAY_BUCKET_DURATIONS_SECONDS = listOf(
    1L, 5L, 10L, 30L, 60L,
    5 * 60L, 15 * 60L, 30 * 60L,
    60 * 60L, 6 * 60 * 60L, 12 * 60 * 60L,
    24 * 60 * 60L, 3 * 24 * 60 * 60L, 7 * 24 * 60 * 60L
)

private const val PERCENTAGE_SCALE = 100f
private const val MIN_BUCKET_DURATION_SECONDS = 1L
private const val LOW_PERCENTAGE_THRESHOLD = 0.1f
private const val MID_PERCENTAGE_THRESHOLD = 10f

private val LOG_LEVEL_SEVERITY_ORDER = listOf(
    LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO,
    LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL, LogLevel.UNKNOWN
)

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

internal fun displayTimeAxisLabelFormatter(
    displayBucketDurationSeconds: Long,
    totalSpanSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): DateTimeFormatter {
    val multiDaySpan = totalSpanSeconds >= 2 * 24 * 60 * 60L
    val pattern = when {
        displayBucketDurationSeconds < 60L -> "HH:mm:ss"
        displayBucketDurationSeconds < 24 * 60 * 60L -> if (multiDaySpan) "MM-dd HH:mm" else "HH:mm"
        totalSpanSeconds >= 365 * 24 * 60 * 60L -> "yyyy-MM-dd"
        else -> "MM-dd"
    }
    return DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
}

internal fun timeBucketRangeFormatter(
    displayBucketDurationSeconds: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): DateTimeFormatter {
    val pattern = if (displayBucketDurationSeconds < 60L) {
        "yyyy-MM-dd HH:mm:ss"
    } else {
        "yyyy-MM-dd HH:mm"
    }
    return DateTimeFormatter.ofPattern(pattern).withZone(zoneId)
}

internal fun timeAxisDateTooltipFormatter(zoneId: ZoneId = ZoneId.systemDefault()): DateTimeFormatter {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)
}

internal fun orderedLevelDistributionSlices(slices: List<DashboardLevelSlice>): List<DashboardLevelSlice> =
    slices.sortedBy { slice -> LOG_LEVEL_SEVERITY_ORDER.indexOf(slice.level) }

internal fun formatLevelDistributionPercentage(ratio: Float): String {
    val normalizedRatio = ratio.coerceIn(0f, 1f)
    if (normalizedRatio == 0f) return "0%"
    val percentage = normalizedRatio * PERCENTAGE_SCALE
    return when {
        percentage < LOW_PERCENTAGE_THRESHOLD -> "<0.1%"
        percentage < MID_PERCENTAGE_THRESHOLD -> String.format(Locale.US, "%.1f%%", percentage)
        else -> "${percentage.roundToInt()}%"
    }
}

internal fun normalizedPieValues(slices: List<DashboardLevelSlice>): List<Float> {
    val nonNegativeRatios = slices.map { slice -> slice.ratio.coerceAtLeast(0f) }
    val ratioSum = nonNegativeRatios.sum()
    if (ratioSum <= 0f) return nonNegativeRatios
    return nonNegativeRatios.map { ratio -> ratio / ratioSum }
}

internal fun chooseDisplayBucketDurationSeconds(
    sortedBuckets: List<DashboardTimeBucket>,
    availableWidthPx: Float,
    minimumPixelsPerBucket: Float = MIN_PIXELS_PER_DISPLAY_BUCKET,
    niceDurationsSeconds: List<Long> = NICE_DISPLAY_BUCKET_DURATIONS_SECONDS
): Long {
    if (sortedBuckets.isEmpty()) return 1L
    val maxVisibleBuckets = maxVisibleBucketCount(
        availableWidthPx = availableWidthPx,
        minimumPixelsPerBucket = minimumPixelsPerBucket
    )
    val totalSpanSeconds = timeSeriesSpanSeconds(sortedBuckets)
    val requiredDurationByWidthSeconds = ceil(totalSpanSeconds.toDouble() / maxVisibleBuckets.toDouble())
        .toLong().coerceAtLeast(1L)
    val longestSourceBucketSeconds = sortedBuckets
        .maxOfOrNull { bucket -> bucketDurationSeconds(bucket) }
        ?.coerceAtLeast(MIN_BUCKET_DURATION_SECONDS) ?: MIN_BUCKET_DURATION_SECONDS
    val minimumRequiredDurationSeconds = maxOf(requiredDurationByWidthSeconds, longestSourceBucketSeconds)
    return chooseNiceDurationAtLeast(
        minimumDurationSeconds = minimumRequiredDurationSeconds,
        niceDurationsSeconds = niceDurationsSeconds
    )
}

private fun maxVisibleBucketCount(availableWidthPx: Float, minimumPixelsPerBucket: Float): Int {
    val safeWidthPx = availableWidthPx.takeIf { width -> width > 0f } ?: DEFAULT_TIME_SERIES_CHART_WIDTH_PX
    return (safeWidthPx / minimumPixelsPerBucket.coerceAtLeast(1f)).toInt().coerceAtLeast(1)
}

private fun chooseNiceDurationAtLeast(
    minimumDurationSeconds: Long,
    niceDurationsSeconds: List<Long>
): Long {
    val sorted = niceDurationsSeconds.filter { it > 0L }.distinct().sorted()
    sorted.firstOrNull { it >= minimumDurationSeconds }?.let { return it }
    var fallback = (sorted.lastOrNull() ?: MIN_BUCKET_DURATION_SECONDS).coerceAtLeast(1L)
    while (fallback < minimumDurationSeconds) {
        fallback *= 2
    }
    return fallback
}
