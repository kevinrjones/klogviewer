package com.logviewer.domain.repository

import arrow.core.Either
import com.logviewer.domain.model.LogFailure
import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogUpdate
import com.logviewer.domain.parser.LogParser
import kotlinx.coroutines.flow.Flow

interface LogSource {
    fun observeLogs(path: LogFilePath, parser: LogParser? = null): Flow<Either<LogFailure, LogUpdate>>
}
