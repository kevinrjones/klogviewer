package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.domain.model.SftpConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

class S3FileSystem(
    private val s3ClientProvider: S3ClientProvider = S3ClientProvider()
) : RemoteFileSystem {

    override suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>> {
        return LogFailure.FileError("S3FileSystem does not support SFTP").left()
    }

    override suspend fun listS3Objects(config: S3Config, prefix: String): Either<LogFailure, List<RemoteFile>> = withContext(Dispatchers.IO) {
        try {
            val client = s3ClientProvider.createClient(config)
            try {
                val response = client.listObjectsV2(ListObjectsV2Request {
                    bucket = config.bucket
                    this.prefix = prefix.removePrefix("/")
                    delimiter = "/"
                })

                val folders = response.commonPrefixes?.map { commonPrefix ->
                    val name = commonPrefix.prefix?.removeSuffix("/")?.substringAfterLast("/") ?: ""
                    RemoteFile(
                        name = name,
                        path = commonPrefix.prefix ?: "",
                        isDirectory = true,
                        size = 0,
                        lastModified = 0
                    )
                } ?: emptyList()

                val objects = response.contents?.map { obj ->
                    val name = obj.key?.substringAfterLast("/") ?: ""
                    val instant = obj.lastModified
                    val epochMs = if (instant != null) {
                        instant.epochSeconds * 1000 + (instant.nanosecondsOfSecond / 1000000)
                    } else 0L
                    
                    RemoteFile(
                        name = name,
                        path = obj.key ?: "",
                        isDirectory = false,
                        size = obj.size ?: 0,
                        lastModified = epochMs
                    )
                }?.filter { it.name.isNotEmpty() } ?: emptyList()

                (folders + objects).right()
            } finally {
                client.close()
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error(e) { "Error listing S3 objects: bucket=${config.bucket}, prefix=$prefix" }
            LogFailure.FileError("Error listing S3 objects: ${e.message}", cause = e).left()
        }
    }
}
