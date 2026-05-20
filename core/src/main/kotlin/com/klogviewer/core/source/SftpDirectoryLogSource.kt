package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.util.withRetry
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class SftpDirectoryLogSource(
    private val config: SftpConfig,
    private val remoteFileSystem: RemoteFileSystem,
    private val rescanIntervalMs: Long = 5000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider(),
    private val logSourceFactory: (SftpConfig, SSHClient?) -> LogSource = { cfg, client ->
        SftpLogSource(cfg, com.klogviewer.core.parser.SimpleLogParser(), existingClient = client)
    }
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow<Either<LogFailure, LogUpdate>> {
        logger.info { "Started observing remote directory: ${path.value} on ${config.host.value}" }

        val activeSources = mutableMapOf<String, Job>()
        val currentEntries = mutableMapOf<String, MutableList<LogEntry>>()
        val filesAttemptedInitial = mutableSetOf<String>()
        var initialized = false

        // Simple connection pool logic
        val clients = mutableListOf<Pair<SSHClient, Int>>()
        val maxSessionsPerClient = 8 // Slightly below typical limit of 10

        suspend fun getOrCreateClient(): SSHClient {
            synchronized(clients) {
                val available = clients.find { it.second < maxSessionsPerClient && it.first.isConnected && it.first.isAuthenticated }
                if (available != null) {
                    val index = clients.indexOf(available)
                    clients[index] = available.first to available.second + 1
                    return available.first
                }
            }

            val client = withRetry(maxRetries = 3) {
                val c = sshClientProvider.createClient()
                c.addHostKeyVerifier(PromiscuousVerifier())
                withContext(dispatcher) {
                    c.connect(config.host.value, config.port.value)
                    try {
                        when (val auth = config.auth) {
                            is SftpAuth.Password -> c.authPassword(config.username.value, auth.password)
                            is SftpAuth.KeyPair -> {
                                val keyProvider = if (!auth.passphrase.isNullOrBlank()) {
                                    c.loadKeys(auth.privateKeyPath, auth.passphrase)
                                } else {
                                    c.loadKeys(auth.privateKeyPath)
                                }
                                c.authPublickey(config.username.value, keyProvider)
                            }
                        }
                    } catch (e: Exception) {
                        try { c.disconnect(); c.close() } catch (_: Exception) {}
                        throw e
                    }
                }
                c
            }
            synchronized(clients) {
                clients.add(client to 1)
            }
            return client
        }

        fun releaseClient(client: SSHClient) {
            synchronized(clients) {
                val entry = clients.find { it.first == client }
                if (entry != null) {
                    val index = clients.indexOf(entry)
                    if (entry.second > 1) {
                        clients[index] = entry.first to entry.second - 1
                    } else {
                        clients.removeAt(index)
                        try { client.disconnect(); client.close() } catch (_: Exception) {}
                    }
                }
            }
        }

        try {
            val sourceIdBase = "sftp://${config.username.value}@${config.host.value}:${config.port.value}"
            val directorySourceId = "$sourceIdBase${path.value}"
            var firstScanPerformed = false
            var currentFilePaths = emptyList<String>()

            while (isActive) {
                // Only scan if not initialized or if it's the first time
                if (!firstScanPerformed || initialized) {
                    val result = remoteFileSystem.listFiles(config, path.value)
                    firstScanPerformed = true

                    result.fold(
                        { failure ->
                            logger.error { "Error scanning remote directory: $failure" }
                            val failureWithSource = when (failure) {
                                is LogFailure.FileError -> failure.copy(sourceId = directorySourceId)
                                is LogFailure.ParsingError -> failure.copy(sourceId = directorySourceId)
                            }
                            send(failureWithSource.left())
                        },
                        { discoveredFiles ->
                            currentFilePaths = discoveredFiles.filter { !it.isDirectory }.map { it.path }
                            val newFiles = currentFilePaths.filter { !activeSources.containsKey(it) }
                            val removedFiles = activeSources.keys.filter { !currentFilePaths.contains(it) }

                            if (removedFiles.isNotEmpty()) {
                                logger.info { "Removing ${removedFiles.size} remote files" }
                                for (file in removedFiles) {
                                    activeSources[file]?.cancel()
                                    activeSources.remove(file)
                                    currentEntries.remove(file)
                                    send(LogUpdate.SourceMissing("$sourceIdBase$file").right())
                                }
                            }

                            if (newFiles.isNotEmpty()) {
                                logger.info { "Discovered ${newFiles.size} new remote files: $newFiles" }
                                newFiles.forEachIndexed { index, file ->
                                    activeSources[file] = launch {
                                        // Add a staggered delay to avoid hammering the SSH server with many simultaneous connection attempts
                                        if (index > 0) delay((index * 200L).milliseconds)

                                        var client: SSHClient? = null
                                        try {
                                            client = getOrCreateClient()
                                            val sftpSource = logSourceFactory(config, client)
                                            sftpSource.observeLogs(LogFilePath(file), parser).collect { fileResult ->
                                                fileResult.fold(
                                                    { failure ->
                                                        logger.error { "Error observing remote file $file: $failure" }
                                                        if (!initialized) {
                                                            filesAttemptedInitial.add(file)
                                                        }
                                                        // For directory monitoring, individual file errors are not terminal failures
                                                        // They will be detected as missing if they truly don't exist
                                                    },
                                                    { update ->
                                                        when (update) {
                                                            is LogUpdate.Initial -> {
                                                                currentEntries.getOrPut(file) { mutableListOf() }.addAll(update.entries)
                                                                if (!initialized) {
                                                                    filesAttemptedInitial.add(file)
                                                                }
                                                                if (initialized) {
                                                                    send(LogUpdate.Appended(update.entries).right())
                                                                }
                                                            }
                                                            is LogUpdate.Appended -> {
                                                                if (!initialized) {
                                                                    currentEntries.getOrPut(file) { mutableListOf() }.addAll(update.entries)
                                                                }
                                                                send(update.right())
                                                            }
                                                            LogUpdate.Reset -> {}
                                                            is LogUpdate.SourceMissing -> send(update.right())
                                                        }
                                                    }
                                                )
                                            }
                                        } finally {
                                            client?.let { releaseClient(it) }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (!initialized && firstScanPerformed && (currentFilePaths.isEmpty() || filesAttemptedInitial.size >= currentFilePaths.size)) {
                    val allEntries = currentEntries.values.flatten().sortedBy { it.timestamp.value }
                    logger.info { "Initial remote directory load complete: ${allEntries.size} entries from ${currentEntries.size}/${currentFilePaths.size} files" }
                    send(LogUpdate.Initial(allEntries).right())
                    initialized = true
                }

                val delayInterval = if (initialized) rescanIntervalMs.milliseconds else 500.milliseconds
                delay(delayInterval)
            }
        } finally {
            synchronized(clients) {
                clients.forEach {
                    try { it.first.disconnect(); it.first.close() } catch (_: Exception) {}
                }
                clients.clear()
            }
        }
    }.flowOn(dispatcher)
}
