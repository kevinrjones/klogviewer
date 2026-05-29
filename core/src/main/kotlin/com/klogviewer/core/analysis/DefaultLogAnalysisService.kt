package com.klogviewer.core.analysis

import arrow.core.Either
import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.FieldFrequencyResult
import com.klogviewer.domain.repository.AnalysisMetricsRepository
import com.klogviewer.domain.repository.LogAnalysisService

class DefaultLogAnalysisService(
    private val metricsRepository: AnalysisMetricsRepository
) : LogAnalysisService {

    override suspend fun frequencyAnalysis(query: FieldFrequencyQuery): Either<AnalysisFailure, FieldFrequencyResult> {
        return metricsRepository.frequencyAnalysis(query)
    }
}
