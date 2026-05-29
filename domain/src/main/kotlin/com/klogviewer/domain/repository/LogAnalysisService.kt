package com.klogviewer.domain.repository

import arrow.core.Either
import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.FieldFrequencyResult
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import com.klogviewer.domain.model.TimeSeriesMetricsResult

interface AnalysisMetricsRepository {
    suspend fun timeSeriesMetrics(query: TimeSeriesMetricsQuery): Either<AnalysisFailure, TimeSeriesMetricsResult>
    suspend fun frequencyAnalysis(query: FieldFrequencyQuery): Either<AnalysisFailure, FieldFrequencyResult>
}

interface LogAnalysisService {
    suspend fun frequencyAnalysis(query: FieldFrequencyQuery): Either<AnalysisFailure, FieldFrequencyResult>
}
