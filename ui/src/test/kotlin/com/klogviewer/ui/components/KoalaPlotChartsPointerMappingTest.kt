package com.klogviewer.ui.components

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.mvi.DashboardLevelSlice
import com.klogviewer.ui.mvi.DashboardTimeBucket
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.time.Instant
import kotlin.math.abs

class KoalaPlotChartsPointerMappingTest {

    @Test
    fun `given invalid width or bucket count when mapping pointer x then returns null`() {
        expectThat(pointerXToBucketIndex(pointerX = 12f, plotWidthPx = 0f, bucketCount = 3)).isNull()
        expectThat(pointerXToBucketIndex(pointerX = 12f, plotWidthPx = 100f, bucketCount = 0)).isNull()
    }

    @Test
    fun `given pointer coordinates when mapping to bucket index then clamps and rounds to nearest index`() {
        val indices = listOf(-20f, 0f, 24f, 50f, 76f, 100f, 120f)
            .map { pointerX ->
                pointerXToBucketIndex(pointerX = pointerX, plotWidthPx = 100f, bucketCount = 5)
            }

        expectThat(indices).containsExactly(0, 0, 1, 2, 3, 4, 4)
    }

    @Test
    fun `given drag coordinates when resolving range then returns normalized inclusive range`() {
        val forwardRange = bucketRangeFromDrag(
            dragStartX = 10f,
            dragEndX = 90f,
            plotWidthPx = 100f,
            bucketCount = 5
        )
        val reverseRange = bucketRangeFromDrag(
            dragStartX = 90f,
            dragEndX = 10f,
            plotWidthPx = 100f,
            bucketCount = 5
        )

        expectThat(forwardRange).isEqualTo(0..4)
        expectThat(reverseRange).isEqualTo(0..4)
    }

    @Test
    fun `given selected bucket when resolving selected index range then returns single index`() {
        val buckets = dashboardBuckets()

        val range = selectedBucketIndexRange(
            sortedBuckets = buckets,
            selectedBucketFrom = buckets[1].from,
            selectedRangeFrom = buckets.first().from,
            selectedRangeTo = buckets.last().to
        )

        expectThat(range).isEqualTo(1..1)
    }

    @Test
    fun `given active range when resolving selected index range then returns inclusive indices`() {
        val buckets = dashboardBuckets()

        val range = selectedBucketIndexRange(
            sortedBuckets = buckets,
            selectedBucketFrom = null,
            selectedRangeFrom = buckets[1].from,
            selectedRangeTo = buckets[2].to
        )

        expectThat(range).isEqualTo(1..2)
    }

    @Test
    fun `given index and selected range when resolving visual state then returns expected state`() {
        expectThat(bucketSelectionVisualState(index = 1, selectedRange = 1..1))
            .isEqualTo(BucketSelectionVisualState.SELECTED)
        expectThat(bucketSelectionVisualState(index = 2, selectedRange = 1..3))
            .isEqualTo(BucketSelectionVisualState.SELECTED_RANGE_ITEM)
        expectThat(bucketSelectionVisualState(index = 0, selectedRange = 1..3))
            .isEqualTo(BucketSelectionVisualState.UNSELECTED)
    }

    @Test
    fun `given drag preview range when resolving active selection then preview range is used`() {
        val range = activeBucketSelectionRange(
            dragStartX = 10f,
            dragCurrentX = 90f,
            plotWidthPx = 100f,
            bucketCount = 5
        )

        expectThat(range).isEqualTo(0..4)
    }

    @Test
    fun `given no drag preview when resolving active selection then no visual selection is returned`() {
        val range = activeBucketSelectionRange(
            dragStartX = null,
            dragCurrentX = null,
            plotWidthPx = 100f,
            bucketCount = 5
        )

        expectThat(range).isNull()
    }

    @Test
    fun `given unsorted level slices when ordering then severity order is stable`() {
        val orderedLevels = orderedLevelDistributionSlices(
            listOf(
                DashboardLevelSlice(level = LogLevel.ERROR, count = 2, ratio = 0.2f),
                DashboardLevelSlice(level = LogLevel.UNKNOWN, count = 1, ratio = 0.1f),
                DashboardLevelSlice(level = LogLevel.DEBUG, count = 3, ratio = 0.3f),
                DashboardLevelSlice(level = LogLevel.FATAL, count = 1, ratio = 0.1f),
                DashboardLevelSlice(level = LogLevel.INFO, count = 4, ratio = 0.4f),
                DashboardLevelSlice(level = LogLevel.WARN, count = 5, ratio = 0.5f)
            )
        ).map { it.level }

        expectThat(orderedLevels).containsExactly(
            LogLevel.DEBUG,
            LogLevel.INFO,
            LogLevel.WARN,
            LogLevel.ERROR,
            LogLevel.FATAL,
            LogLevel.UNKNOWN
        )
    }

    @Test
    fun `given level ratio when formatting percentage then tiny and regular values are represented clearly`() {
        expectThat(formatLevelDistributionPercentage(0f)).isEqualTo("0%")
        expectThat(formatLevelDistributionPercentage(0.0005f)).isEqualTo("<0.1%")
        expectThat(formatLevelDistributionPercentage(0.004f)).isEqualTo("0.4%")
        expectThat(formatLevelDistributionPercentage(0.076f)).isEqualTo("7.6%")
        expectThat(formatLevelDistributionPercentage(0.125f)).isEqualTo("13%")
    }

    @Test
    fun `given skewed non-normalized slices when deriving pie values then values remain visible and normalized`() {
        val values = normalizedPieValues(
            listOf(
                DashboardLevelSlice(level = LogLevel.DEBUG, count = 163000, ratio = 0.994f),
                DashboardLevelSlice(level = LogLevel.INFO, count = 640, ratio = 0.0039f),
                DashboardLevelSlice(level = LogLevel.WARN, count = 120, ratio = 0.0007f),
                DashboardLevelSlice(level = LogLevel.ERROR, count = 61, ratio = 0.0003f)
            )
        )

        expectThat(values.size).isEqualTo(4)
        expectThat(abs(values.sum() - 1f) < 0.0001f).isEqualTo(true)
        expectThat(abs(values[0] - 0.9941f) < 0.001f).isEqualTo(true)
        expectThat(abs(values[1] - 0.0039f) < 0.001f).isEqualTo(true)
        expectThat(abs(values[2] - 0.0007f) < 0.001f).isEqualTo(true)
        expectThat(abs(values[3] - 0.0003f) < 0.001f).isEqualTo(true)
    }

    @Test
    fun `given non-positive ratios when deriving pie values then normalization remains safe`() {
        val values = normalizedPieValues(
            listOf(
                DashboardLevelSlice(level = LogLevel.DEBUG, count = 0, ratio = -1f),
                DashboardLevelSlice(level = LogLevel.INFO, count = 0, ratio = 0f)
            )
        )

        expectThat(values).containsExactly(0f, 0f)
    }

    private fun dashboardBuckets(): List<DashboardTimeBucket> {
        return listOf(
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T10:00:00Z"),
                to = Instant.parse("2026-05-27T10:01:00Z"),
                count = 4
            ),
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T10:01:00Z"),
                to = Instant.parse("2026-05-27T10:02:00Z"),
                count = 6
            ),
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T10:02:00Z"),
                to = Instant.parse("2026-05-27T10:03:00Z"),
                count = 8
            )
        )
    }
}
