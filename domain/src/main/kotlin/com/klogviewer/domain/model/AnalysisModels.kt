package com.klogviewer.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@JvmInline
value class TimeBucketSize private constructor(val duration: Duration) {
    companion object {
        val ONE_SECOND: TimeBucketSize = TimeBucketSize(1.seconds)
        val ONE_MINUTE: TimeBucketSize = TimeBucketSize(1.minutes)

        fun from(duration: Duration): Either<AnalysisFailure, TimeBucketSize> {
            return if (duration <= Duration.ZERO) {
                AnalysisFailure.InvalidTimeBucketSize(duration).left()
            } else {
                TimeBucketSize(duration).right()
            }
        }
    }
}

@JvmInline
value class AnalysisFieldKey private constructor(val value: String) {
    companion object {
        fun from(value: String): Either<AnalysisFailure, AnalysisFieldKey> {
            return if (value.isBlank()) {
                AnalysisFailure.InvalidFieldKey(value).left()
            } else {
                AnalysisFieldKey(value.trim()).right()
            }
        }
    }
}

@JvmInline
value class FrequencyCount private constructor(val value: Int) {
    companion object {
        val ZERO: FrequencyCount = FrequencyCount(0)

        fun from(value: Int): Either<AnalysisFailure, FrequencyCount> {
            return if (value < 0) {
                AnalysisFailure.InvalidFrequencyCount(value).left()
            } else {
                FrequencyCount(value).right()
            }
        }
    }
}

data class DiffWindow(
    val from: Instant?,
    val to: Instant?
) {
    companion object {
        val Unbounded: DiffWindow = DiffWindow(from = null, to = null)
    }

    fun contains(instant: Instant): Boolean {
        val afterFrom = from?.let { !instant.isBefore(it) } ?: true
        val beforeTo = to?.let { !instant.isAfter(it) } ?: true
        return afterFrom && beforeTo
    }

    fun validate(): Either<AnalysisFailure, DiffWindow> {
        return if (from != null && to != null && from.isAfter(to)) {
            AnalysisFailure.InvalidDiffWindow(from = from.toString(), to = to.toString()).left()
        } else {
            this.right()
        }
    }
}

data class TimeBucketWindow(
    val from: Instant,
    val to: Instant
)

data class TimeSeriesBucket(
    val window: TimeBucketWindow,
    val count: FrequencyCount,
    val entries: List<LogEntry>
)

data class TimeSeriesMetricsResult(
    val bucketSize: TimeBucketSize,
    val buckets: List<TimeSeriesBucket>
)

data class TimeSeriesMetricsQuery(
    val entries: List<LogEntry>,
    val bucketSize: TimeBucketSize = TimeBucketSize.ONE_SECOND,
    val window: DiffWindow = DiffWindow.Unbounded
)

data class FieldFrequencyItem(
    val value: String,
    val count: FrequencyCount
)

data class FieldFrequencyQuery(
    val entries: List<LogEntry>,
    val fieldKey: AnalysisFieldKey,
    val limit: Int = 20,
    val window: DiffWindow = DiffWindow.Unbounded
)

data class FieldFrequencyResult(
    val fieldKey: AnalysisFieldKey,
    val frequencies: List<FieldFrequencyItem>
)

data class DashboardMetrics(
    val timeSeries: TimeSeriesMetricsResult
)
