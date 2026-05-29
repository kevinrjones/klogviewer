package com.klogviewer.ui.components

import com.klogviewer.ui.mvi.DashboardBucketSize
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Instant
import java.time.ZoneId

class KoalaPlotChartsFormattingTest {

    @Test
    fun `given per second bucket size when formatting x-axis time then includes seconds`() {
        val formatter = timeAxisLabelFormatter(
            bucketSize = DashboardBucketSize.PER_SECOND,
            zoneId = ZoneId.of("UTC")
        )

        expectThat(formatter.format(Instant.parse("2026-05-27T10:01:05Z")))
            .isEqualTo("10:01:05")
    }

    @Test
    fun `given per minute bucket size when formatting x-axis time then omits seconds`() {
        val formatter = timeAxisLabelFormatter(
            bucketSize = DashboardBucketSize.PER_MINUTE,
            zoneId = ZoneId.of("UTC")
        )

        expectThat(formatter.format(Instant.parse("2026-05-27T10:01:59Z")))
            .isEqualTo("10:01")
    }

    @Test
    fun `given bucket instant when formatting x-axis tooltip date then uses yyyy-mm-dd`() {
        val formatter = timeAxisDateTooltipFormatter(zoneId = ZoneId.of("UTC"))

        expectThat(formatter.format(Instant.parse("2026-05-27T23:30:00Z")))
            .isEqualTo("2026-05-27")
    }

    @Test
    fun `given non utc timezone when formatting x-axis tooltip date then applies timezone shift`() {
        val formatter = timeAxisDateTooltipFormatter(zoneId = ZoneId.of("Asia/Tokyo"))

        expectThat(formatter.format(Instant.parse("2026-05-27T23:30:00Z")))
            .isEqualTo("2026-05-28")
    }
}
