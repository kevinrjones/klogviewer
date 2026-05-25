package com.klogviewer.domain.model

import kotlin.time.Duration

sealed interface AnalysisFailure {
    data object NoTimestampData : AnalysisFailure
    data class InvalidTimeBucketSize(val duration: Duration) : AnalysisFailure
    data class InvalidFieldKey(val value: String) : AnalysisFailure
    data class InvalidFrequencyCount(val value: Int) : AnalysisFailure
    data class InvalidDiffWindow(val from: String?, val to: String?) : AnalysisFailure
    data class FieldUnavailable(val fieldKey: AnalysisFieldKey) : AnalysisFailure
    data class Unexpected(val message: String) : AnalysisFailure
}
