package com.klogviewer.domain.repository

import arrow.core.Either
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig

interface RemoteFileSystem {
    suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>>
    suspend fun listS3Objects(config: S3Config, prefix: String): Either<LogFailure, List<RemoteFile>>

    suspend fun isS3Directory(config: S3Config, prefix: String): Boolean {
        if (prefix.endsWith("/")) return true
        return listS3Objects(config, prefix).fold(
            { false },
            { objects ->
                objects.size > 1 || (objects.size == 1 && objects[0].path != prefix.removePrefix("/"))
            }
        )
    }

    suspend fun isSftpDirectory(config: SftpConfig, path: String): Boolean {
        if (path.endsWith("/")) return true
        return listFiles(config, path).fold(
            { false },
            { files ->
                files.size > 1 || files.isEmpty() || (files.size == 1 && files[0].path.removeSuffix("/") != path.removeSuffix("/"))
            }
        )
    }
}
