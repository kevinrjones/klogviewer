package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.util.withRetry
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.RemoteFile
import com.klogviewer.domain.model.SftpAuth
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.transport.verification.PromiscuousVerifier

private val logger = KotlinLogging.logger {}

class SftpFileSystem(
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider()
) : RemoteFileSystem {

    override suspend fun listFiles(config: SftpConfig, path: String): Either<LogFailure, List<RemoteFile>> = withContext(Dispatchers.IO) {
        try {
            withRetry(maxRetries = 3) {
                val client = sshClientProvider.createClient()
                client.addHostKeyVerifier(PromiscuousVerifier())
                
                try {
                    client.connect(config.host.value, config.port.value)
                    
                    when (val auth = config.auth) {
                        is SftpAuth.Password -> client.authPassword(config.username.value, auth.password)
                        is SftpAuth.KeyPair -> {
                            val keyProvider = if (!auth.passphrase.isNullOrBlank()) {
                                client.loadKeys(auth.privateKeyPath, auth.passphrase)
                            } else {
                                client.loadKeys(auth.privateKeyPath)
                            }
                            client.authPublickey(config.username.value, keyProvider)
                        }
                    }

                    client.newSFTPClient().use { sftp ->
                        val entries = sftp.ls(path)
                        entries.map { entry ->
                            RemoteFile(
                                name = entry.name,
                                path = if (path.endsWith("/")) "$path${entry.name}" else "$path/${entry.name}",
                                isDirectory = entry.isDirectory,
                                size = entry.attributes.size,
                                lastModified = entry.attributes.mtime
                            )
                        }
                    }
                } finally {
                    try {
                        client.disconnect()
                        client.close()
                    } catch (e: Exception) {
                        logger.warn { "Error disconnecting SSH client: ${e.message}" }
                    }
                }
            }.right()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            logger.error(e) { "Error listing remote directory: $path" }
            LogFailure.FileError("Error listing remote directory: ${e.message}", cause = e).left()
        }
    }
}
