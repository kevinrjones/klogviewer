package com.klogviewer.domain.repository

import arrow.core.Either
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.SftpConfig

interface RemoteFileSystem {
    suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>>
}
