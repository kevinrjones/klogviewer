package com.klogviewer.core.analysis

import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.AnalysisFieldKey
import com.klogviewer.domain.model.DiffWindow
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.time.Instant

class InMemoryAnalysisMetricsRepositoryTest {

    private val repository = InMemoryAnalysisMetricsRepository()

    @Test
    suspend fun `given timestamped logs when computing time series then returns bucketed counts in order`() {
        val entries = listOf(
            logEntry("2026-01-01T00:00:00Z", "first"),
            logEntry("2026-01-01T00:00:00Z", "second"),
            logEntry("2026-01-01T00:00:01Z", "third")
        )

        val result = repository.timeSeriesMetrics(TimeSeriesMetricsQuery(entries = entries))

        result.fold(
            ifLeft = { failure -> throw AssertionError("Expected success but was $failure") },
            ifRight = { metrics ->
                expectThat(metrics.buckets).hasSize(2)
                expectThat(metrics.buckets.map { it.count.value }).containsExactly(2, 1)
                expectThat(metrics.buckets.map { it.window.from }).containsExactly(
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-01T00:00:01Z")
                )
            }
        )
    }

    @Test
    suspend fun `given logs without parsed instants when computing time series then returns no timestamp failure`() {
        val entries = listOf(
            LogEntry(
                timestamp = LogTimestamp("2026-01-01T00:00:00"),
                level = LogLevel.INFO,
                content = LogContent("missing instant")
            )
        )

        val result = repository.timeSeriesMetrics(TimeSeriesMetricsQuery(entries = entries))

        result.fold(
            ifLeft = { failure ->
                expectThat(failure).isEqualTo(AnalysisFailure.NoTimestampData)
            },
            ifRight = {
                throw AssertionError("Expected failure but got success")
            }
        )
    }

    @Test
    suspend fun `given field frequency query when entries include missing values then missing bucket is returned`() {
        val fieldKey = AnalysisFieldKey.from("requestId").fold(
            ifLeft = { failure -> throw AssertionError("Unexpected failure creating key: $failure") },
            ifRight = { it }
        )
        val entries = listOf(
            logEntry("2026-01-01T00:00:00Z", "one", fields = mapOf("requestId" to "A")),
            logEntry("2026-01-01T00:00:01Z", "two", fields = mapOf("requestId" to "A")),
            logEntry("2026-01-01T00:00:02Z", "three", fields = emptyMap())
        )

        val result = repository.frequencyAnalysis(
            com.klogviewer.domain.model.FieldFrequencyQuery(
                entries = entries,
                fieldKey = fieldKey,
                window = DiffWindow.Unbounded
            )
        )

        result.fold(
            ifLeft = { failure -> throw AssertionError("Expected success but was $failure") },
            ifRight = { frequency ->
                expectThat(frequency.frequencies.map { it.value }).containsExactly("A", "(missing)")
                expectThat(frequency.frequencies.map { it.count.value }).containsExactly(2, 1)
            }
        )
    }

    private fun logEntry(timestamp: String, content: String, fields: Map<String, String> = emptyMap()): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp(timestamp),
            level = LogLevel.INFO,
            content = LogContent(content),
            fields = fields,
            instant = Instant.parse(timestamp)
        )
    }
}
