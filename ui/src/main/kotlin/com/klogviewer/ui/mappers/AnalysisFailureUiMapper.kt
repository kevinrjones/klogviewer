package com.klogviewer.ui.mappers

import com.klogviewer.domain.model.AnalysisFailure

fun AnalysisFailure.toUiMessage(): String {
    return when (this) {
        AnalysisFailure.NoTimestampData -> "Dashboard requires logs with parsed timestamps"
        is AnalysisFailure.InvalidTimeBucketSize -> "Invalid dashboard bucket size"
        is AnalysisFailure.InvalidFieldKey -> "Invalid field selected for frequency analysis"
        is AnalysisFailure.InvalidFrequencyCount -> "Invalid frequency count generated"
        is AnalysisFailure.InvalidDiffWindow -> "Invalid dashboard date range"
        is AnalysisFailure.FieldUnavailable -> "Selected field is unavailable in the current log window"
        is AnalysisFailure.Unexpected -> message
    }
}
