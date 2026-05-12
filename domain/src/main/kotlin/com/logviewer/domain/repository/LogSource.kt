package com.logviewer.domain.repository

import arrow.core.Either
import com.logviewer.domain.model.LogFailure
import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogUpdate
import kotlinx.coroutines.flow.Flow

interface LogSource {
    fun observeLogs(path: LogFilePath): Flow<Either<LogFailure, LogUpdate>>
}
