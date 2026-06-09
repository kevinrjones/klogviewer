package com.klogviewer.core.analysis

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.FieldFrequencyItem
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.FieldFrequencyResult
import com.klogviewer.domain.model.FrequencyCount
import com.klogviewer.domain.model.TimeBucketWindow
import com.klogviewer.domain.model.TimeSeriesBucket
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import com.klogviewer.domain.model.TimeSeriesMetricsResult
import com.klogviewer.domain.repository.AnalysisMetricsRepository
import java.time.Instant

class InMemoryAnalysisMetricsRepository : AnalysisMetricsRepository {

    override suspend fun timeSeriesMetrics(query: TimeSeriesMetricsQuery): Either<AnalysisFailure, TimeSeriesMetricsResult> {
        return query.window.validate().flatMap {
            val entriesWithTimestamp = query.entries.mapNotNull { entry ->
                entry.instant?.let { instant -> instant to entry }
            }
            if (entriesWithTimestamp.isEmpty()) {
                return AnalysisFailure.NoTimestampData.left()
            }

            val filteredEntries = entriesWithTimestamp
                .filter { (instant, _) -> query.window.contains(instant) }
                .map { it.second }

            if (filteredEntries.isEmpty()) {
                return TimeSeriesMetricsResult(
                    bucketSize = query.bucketSize,
                    buckets = emptyList()
                ).right()
            }

            val bucketDurationMillis = query.bucketSize.duration.inWholeMilliseconds
            val bucketsByStart = filteredEntries
                .groupBy { entry ->
                    val instant = requireNotNull(entry.instant)
                    bucketStart(instant, bucketDurationMillis)
                }
                .toSortedMap()

            val initialBuckets: Either<AnalysisFailure, List<TimeSeriesBucket>> = emptyList<TimeSeriesBucket>().right()
            val buckets = bucketsByStart.entries.fold(initialBuckets) { accEither, (start, bucketEntries) ->
                accEither.flatMap { acc ->
                    FrequencyCount.from(bucketEntries.size).fold(
                        ifLeft = { it.left() },
                        ifRight = { count ->
                            (
                                acc + TimeSeriesBucket(
                                    window = TimeBucketWindow(
                                        from = start,
                                        to = start.plusMillis(bucketDurationMillis)
                                    ),
                                    count = count,
                                    entries = bucketEntries
                                )
                            ).right()
                        }
                    )
                }
            }

            buckets.fold(
                ifLeft = { it.left() },
                ifRight = { bucketList ->
                    TimeSeriesMetricsResult(
                        bucketSize = query.bucketSize,
                        buckets = bucketList
                    ).right()
                }
            )
        }
    }

    override suspend fun frequencyAnalysis(query: FieldFrequencyQuery): Either<AnalysisFailure, FieldFrequencyResult> {
        return query.window.validate().flatMap {
            val filteredEntries = query.entries.filter { entry ->
                val isUnbounded = query.window.from == null && query.window.to == null
                if (isUnbounded) {
                    true
                } else {
                    entry.instant?.let(query.window::contains) ?: false
                }
            }

            val frequencies = filteredEntries
                .groupingBy { entry ->
                    entry.compatibilityFields()[query.fieldKey.value]
                        ?.takeIf { value -> value.isNotBlank() }
                        ?: "(missing)"
                }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(if (query.limit <= 0) Int.MAX_VALUE else query.limit)

            val initialFrequencyItems: Either<AnalysisFailure, List<FieldFrequencyItem>> = emptyList<FieldFrequencyItem>().right()
            val frequencyItems = frequencies.fold(initialFrequencyItems) { accEither, item ->
                accEither.flatMap { acc ->
                    FrequencyCount.from(item.value).fold(
                        ifLeft = { it.left() },
                        ifRight = { count ->
                            (
                                acc + FieldFrequencyItem(
                                    value = item.key,
                                    count = count
                                )
                            ).right()
                        }
                    )
                }
            }

            frequencyItems.fold(
                ifLeft = { it.left() },
                ifRight = { items ->
                    FieldFrequencyResult(
                        fieldKey = query.fieldKey,
                        frequencies = items
                    ).right()
                }
            )
        }
    }

    private fun bucketStart(instant: Instant, durationMillis: Long): Instant {
        val epochMillis = instant.toEpochMilli()
        val startMillis = (epochMillis / durationMillis) * durationMillis
        return Instant.ofEpochMilli(startMillis)
    }
}
