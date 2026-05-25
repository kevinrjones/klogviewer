package com.klogviewer.core.analysis

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.DashboardMetrics
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.FieldFrequencyResult
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import com.klogviewer.domain.repository.AnalysisMetricsRepository
import com.klogviewer.domain.repository.LogAnalysisService

class DefaultLogAnalysisService(
    private val metricsRepository: AnalysisMetricsRepository
) : LogAnalysisService {

    override suspend fun dashboardMetrics(query: TimeSeriesMetricsQuery): Either<AnalysisFailure, DashboardMetrics> {
        return metricsRepository.timeSeriesMetrics(query).fold(
            ifLeft = { it.left() },
            ifRight = { metrics -> DashboardMetrics(timeSeries = metrics).right() }
        )
    }

    override suspend fun frequencyAnalysis(query: FieldFrequencyQuery): Either<AnalysisFailure, FieldFrequencyResult> {
        return metricsRepository.frequencyAnalysis(query)
    }
}
