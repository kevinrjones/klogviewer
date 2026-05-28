package com.klogviewer.ui.components

import com.klogviewer.ui.mvi.DashboardTimeBucket
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.time.Instant

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
