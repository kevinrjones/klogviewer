package com.klogviewer.core.source

import arrow.core.Either
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.repository.RemoteFileSystem

class UnifiedRemoteFileSystem(
    private val sftpFileSystem: SftpFileSystem = SftpFileSystem(),
    private val s3FileSystem: S3FileSystem = S3FileSystem()
) : RemoteFileSystem {
    override suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>> {
        return sftpFileSystem.listFiles(config, path)
    }

    override suspend fun listS3Objects(config: S3Config, prefix: String): Either<LogFailure, List<RemoteFile>> {
        return s3FileSystem.listS3Objects(config, prefix)
    }
}
