package com.klogviewer.ui.components

import com.klogviewer.ui.mvi.DashboardBucketSize
import com.klogviewer.ui.mvi.DashboardDataState
import com.klogviewer.ui.mvi.DashboardTimeBucket
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.time.Instant

class LogWorkspaceChartSupportTest {

    @Test
    fun `given non content dashboard state when resolving log time frequency content then returns null`() {
        expectThat(logTimeFrequencyContent(DashboardDataState.Loading)).isNull()
        expectThat(logTimeFrequencyContent(DashboardDataState.Empty)).isNull()
        expectThat(logTimeFrequencyContent(DashboardDataState.Error("failed"))).isNull()
    }

    @Test
    fun `given content without time series when resolving log time frequency content then returns null`() {
        val content = dashboardContent(timeSeries = emptyList())

        expectThat(logTimeFrequencyContent(content)).isNull()
    }

    @Test
    fun `given content with time series when resolving log time frequency content then returns content`() {
        val bucket = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:00:00Z"),
            to = Instant.parse("2026-05-27T12:01:00Z"),
            count = 5
        )
        val content = dashboardContent(timeSeries = listOf(bucket))

        expectThat(logTimeFrequencyContent(content)).isEqualTo(content)
    }

    @Test
    fun `given selected bucket when resolving dashboard time selection then returns bucket label`() {
        val bucket1 = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:00:00Z"),
            to = Instant.parse("2026-05-27T12:01:00Z"),
            count = 5
        )
        val bucket2 = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:01:00Z"),
            to = Instant.parse("2026-05-27T12:02:00Z"),
            count = 3
        )
        val content = dashboardContent(
            timeSeries = listOf(bucket1, bucket2),
            selectedBucketFrom = bucket2.from
        )

        val selection = resolveDashboardTimeSelection(
            content = content,
            activeTimeFilterFrom = bucket1.from,
            activeTimeFilterTo = bucket2.to
        )

        expectThat(selection?.label).isEqualTo("Bucket: ${bucket2.from} → ${bucket2.to}")
    }

    @Test
    fun `given active range without selected bucket when resolving dashboard time selection then returns range label`() {
        val bucket1 = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:00:00Z"),
            to = Instant.parse("2026-05-27T12:01:00Z"),
            count = 5
        )
        val bucket2 = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:01:00Z"),
            to = Instant.parse("2026-05-27T12:02:00Z"),
            count = 3
        )
        val content = dashboardContent(timeSeries = listOf(bucket1, bucket2))

        val selection = resolveDashboardTimeSelection(
            content = content,
            activeTimeFilterFrom = bucket1.from,
            activeTimeFilterTo = bucket2.to
        )

        expectThat(selection?.label).isEqualTo("Range: ${bucket1.from} → ${bucket2.to}")
    }

    @Test
    fun `given no matching range when resolving dashboard time selection then returns null`() {
        val bucket = DashboardTimeBucket(
            from = Instant.parse("2026-05-27T12:00:00Z"),
            to = Instant.parse("2026-05-27T12:01:00Z"),
            count = 5
        )
        val content = dashboardContent(timeSeries = listOf(bucket))

        val selection = resolveDashboardTimeSelection(
            content = content,
            activeTimeFilterFrom = Instant.parse("2026-05-27T13:00:00Z"),
            activeTimeFilterTo = Instant.parse("2026-05-27T13:01:00Z")
        )

        expectThat(selection).isNull()
    }

    private fun dashboardContent(
        timeSeries: List<DashboardTimeBucket>,
        selectedBucketFrom: Instant? = null
    ): DashboardDataState.Content {
        return DashboardDataState.Content(
            bucketSize = DashboardBucketSize.PER_MINUTE,
            totalEvents = timeSeries.sumOf { it.count },
            timeSeries = timeSeries,
            levelDistribution = emptyList(),
            selectedBucketFrom = selectedBucketFrom
        )
    }
}
