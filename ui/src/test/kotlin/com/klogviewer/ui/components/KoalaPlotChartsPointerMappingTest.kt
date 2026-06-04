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
    fun `given irregular time buckets when deriving x-axis values then values map to display bucket indices`() {
        val irregularBuckets = listOf(
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T10:57:00Z"),
                to = Instant.parse("2026-05-27T10:58:00Z"),
                count = 2
            ),
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T10:58:00Z"),
                to = Instant.parse("2026-05-27T10:59:00Z"),
                count = 3
            ),
            DashboardTimeBucket(
                from = Instant.parse("2026-05-27T15:27:00Z"),
                to = Instant.parse("2026-05-27T15:28:00Z"),
                count = 4
            )
        )

        expectThat(timeSeriesXAxisValues(irregularBuckets))
            .containsExactly(0f, 1f, 2f)
    }

    @Test
    fun `given short span and wide chart when choosing display bucket duration then per second granularity is preserved`() {
        val start = Instant.parse("2026-05-27T10:00:00Z")
        val buckets = (0 until 120).map { index ->
            val bucketStart = start.plusSeconds(index.toLong())
            DashboardTimeBucket(
                from = bucketStart,
                to = bucketStart.plusSeconds(1),
                count = 1
            )
        }

        val durationSeconds = chooseDisplayBucketDurationSeconds(
            sortedBuckets = buckets,
            availableWidthPx = 4000f
        )

        expectThat(durationSeconds).isEqualTo(1L)
    }

    @Test
    fun `given ten day span and four thousand pixel chart when choosing display bucket duration then wider buckets are selected`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val buckets = listOf(
            DashboardTimeBucket(
                from = start,
                to = start.plusSeconds(60),
                count = 2
            ),
            DashboardTimeBucket(
                from = start.plusSeconds(10 * 24 * 60 * 60L),
                to = start.plusSeconds(10 * 24 * 60 * 60L + 60),
                count = 3
            )
        )

        val durationSeconds = chooseDisplayBucketDurationSeconds(
            sortedBuckets = buckets,
            availableWidthPx = 4000f
        )

        expectThat(durationSeconds).isEqualTo(30 * 60L)
    }

    @Test
    fun `given narrow chart width when choosing display bucket duration then chooses wider buckets`() {
        val start = Instant.parse("2026-05-27T00:00:00Z")
        val buckets = (0 until 3600).map { index ->
            val bucketStart = start.plusSeconds(index.toLong())
            DashboardTimeBucket(
                from = bucketStart,
                to = bucketStart.plusSeconds(1),
                count = 1
            )
        }

        val durationSeconds = chooseDisplayBucketDurationSeconds(
            sortedBuckets = buckets,
            availableWidthPx = 100f
        )

        expectThat(durationSeconds).isEqualTo(5 * 60L)
    }

    @Test
    fun `given empty series when choosing display bucket duration then returns safe default`() {
        val durationSeconds = chooseDisplayBucketDurationSeconds(
            sortedBuckets = emptyList(),
            availableWidthPx = 200f
        )

        expectThat(durationSeconds).isEqualTo(1L)
    }

    @Test
    fun `given sparse long span when rebucketing for display then counts are preserved and zero buckets remain explicit`() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val sourceBuckets = listOf(
            DashboardTimeBucket(
                from = start,
                to = start.plusSeconds(60),
                count = 7
            ),
            DashboardTimeBucket(
                from = start.plusSeconds(9 * 24 * 60 * 60L),
                to = start.plusSeconds(9 * 24 * 60 * 60L + 60),
                count = 11
            )
        )

        val displayBuckets = rebucketTimeSeriesForDisplay(
            sortedBuckets = sourceBuckets,
            displayBucketDurationSeconds = 24 * 60 * 60L
        )

        expectThat(displayBuckets.first().from).isEqualTo(start)
        expectThat(displayBuckets.last().to).isEqualTo(start.plusSeconds(10 * 24 * 60 * 60L))
        expectThat(displayBuckets.sumOf { bucket -> bucket.count }).isEqualTo(sourceBuckets.sumOf { bucket -> bucket.count })
        expectThat(displayBuckets.any { bucket -> bucket.count == 0 }).isEqualTo(true)
        expectThat(displayBuckets.first().count).isEqualTo(7)
        expectThat(displayBuckets.last().count).isEqualTo(11)
    }

    @Test
    fun `given multiple source buckets in same display interval when rebucketing then counts are summed into one display bucket`() {
        val start = Instant.parse("2026-05-27T10:00:00Z")
        val sourceBuckets = listOf(
            DashboardTimeBucket(
                from = start,
                to = start.plusSeconds(60),
                count = 2
            ),
            DashboardTimeBucket(
                from = start.plusSeconds(60),
                to = start.plusSeconds(120),
                count = 3
            ),
            DashboardTimeBucket(
                from = start.plusSeconds(300),
                to = start.plusSeconds(360),
                count = 5
            )
        )

        val displayBuckets = rebucketTimeSeriesForDisplay(
            sortedBuckets = sourceBuckets,
            displayBucketDurationSeconds = 5 * 60L
        )

        expectThat(displayBuckets.size).isEqualTo(2)
        expectThat(displayBuckets[0].from).isEqualTo(start)
        expectThat(displayBuckets[0].to).isEqualTo(start.plusSeconds(5 * 60L))
        expectThat(displayBuckets[0].count).isEqualTo(5)
        expectThat(displayBuckets[1].count).isEqualTo(5)
        expectThat(displayBuckets.sumOf { bucket -> bucket.count }).isEqualTo(10)
    }

    @Test
    fun `given many merged buckets when deriving x-axis range then range includes outer bar width`() {
        val start = Instant.parse("2026-05-27T10:00:00Z")
        val buckets = (0 until 30).map { index ->
            val bucketStart = start.plusSeconds(index.toLong() * 60)
            DashboardTimeBucket(
                from = bucketStart,
                to = bucketStart.plusSeconds(60),
                count = index + 1
            )
        }

        val xValues = timeSeriesXAxisValues(buckets)
        val xRange = timeSeriesXAxisRange(xValues)

        expectThat(abs(xRange.start - (-0.5f)) < 0.0001f).isEqualTo(true)
        expectThat(abs(xRange.endInclusive - 29.5f) < 0.0001f).isEqualTo(true)
    }

    @Test
    fun `given single bucket when deriving x-axis range then range remains centered around bucket`() {
        val xRange = timeSeriesXAxisRange(listOf(0f))

        expectThat(abs(xRange.start - (-0.5f)) < 0.0001f).isEqualTo(true)
        expectThat(abs(xRange.endInclusive - 0.5f) < 0.0001f).isEqualTo(true)
    }

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
