package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

class SftpFileSystem(
    private val sshService: SshService = SshService()
) : RemoteFileSystem {

    override suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>> = withContext(Dispatchers.IO) {
        try {
            val client = sshService.connectAndAuthenticate(config)
            
            try {
                client.newSFTPClient().use { sftp ->
                    val entries = sftp.ls(path)
                    entries
                        .filter { it.name != "." && it.name != ".." }
                        .map { entry ->
                            RemoteFile(
                                name = entry.name,
                                path = if (path.endsWith("/")) "$path${entry.name}" else "$path/${entry.name}",
                                isDirectory = entry.isDirectory,
                                size = entry.attributes.size,
                                lastModified = entry.attributes.mtime
                            )
                        }
                }.right()
            } finally {
                try {
                    client.disconnect()
                    client.close()
                } catch (e: Exception) {
                    logger.warn { "Error disconnecting SSH client: ${e.message}" }
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error(e) { "Error listing remote directory: $path" }
            LogFailure.FileError("Error listing remote directory: ${e.message}", cause = e).left()
        }
    }
}
