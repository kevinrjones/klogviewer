package com.klogviewer.domain.repository

import arrow.core.Either
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.LogFilePath
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.parser.LogParser
import kotlinx.coroutines.flow.Flow

interface LogSource {
    fun observeLogs(path: LogFilePath, parser: LogParser? = null): Flow<Either<LogFailure, LogUpdate>>
}
