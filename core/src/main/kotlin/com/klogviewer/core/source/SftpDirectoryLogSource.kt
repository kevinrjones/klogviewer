package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.schmizz.sshj.SSHClient
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class SftpDirectoryLogSource(
    private val config: SftpConfig,
    private val remoteFileSystem: RemoteFileSystem,
    private val rescanIntervalMs: Long = 5000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sshService: SshService = SshService(),
    private val logSourceFactory: (SftpConfig, SSHClient?) -> LogSource = { cfg, client ->
        SftpLogSource(cfg, com.klogviewer.core.parser.SimpleLogParser(), sshService = sshService, existingClient = client)
    }
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow<Either<LogFailure, LogUpdate>> {
        logger.info { "Started observing remote directory: ${path.value} on ${config.host.value}" }

        val activeSources = mutableMapOf<String, Job>()
        var initialized = false
        val pool = SshClientPool(config, sshService)
        val coordinator = LogInitialLoadCoordinator()

        try {
            val sourceIdBase = "sftp://${config.username.value}@${config.host.value}:${config.port.value}"
            val directorySourceId = "$sourceIdBase${path.value}"
            var firstScanPerformed = false
            var currentFilePaths = emptyList<String>()

            while (isActive) {
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
                                    send(LogUpdate.SourceMissing("$sourceIdBase$file").right())
                                }
                            }

                            if (newFiles.isNotEmpty()) {
                                logger.info { "Discovered ${newFiles.size} new remote files: $newFiles" }
                                newFiles.forEachIndexed { index, file ->
                                    activeSources[file] = launch {
                                        if (index > 0) delay((index * 200L).milliseconds)

                                        var client: SSHClient? = null
                                        try {
                                            client = pool.getOrCreateClient()
                                            val sftpSource = logSourceFactory(config, client)
                                            sftpSource.observeLogs(LogFilePath(file), parser).collect { fileResult ->
                                                fileResult.fold(
                                                    { failure ->
                                                        logger.error { "Error observing remote file $file: $failure" }
                                                        if (!initialized) {
                                                            coordinator.onFileFailedInitial(file)
                                                        }
                                                    },
                                                    { update ->
                                                        when (update) {
                                                            is LogUpdate.Initial -> {
                                                                if (!initialized) {
                                                                    coordinator.onInitialLoad(file, update.entries)
                                                                } else {
                                                                    send(LogUpdate.Appended(update.entries).right())
                                                                }
                                                            }
                                                            is LogUpdate.Appended -> {
                                                                if (!initialized) {
                                                                    coordinator.onAppendedDuringInitial(file, update.entries)
                                                                }
                                                                send(update.right())
                                                            }
                                                            LogUpdate.Reset -> {}
                                                            is LogUpdate.SourceMissing -> send(update.right())
                                                        }
                                                    }
                                                )
                                            }
                                        } catch (e: Exception) {
                                            if (e is CancellationException) throw e
                                            logger.error(e) { "Failed to get client or observe file $file" }
                                            if (!initialized) coordinator.onFileFailedInitial(file)
                                        } finally {
                                            client?.let { pool.releaseClient(it) }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (!initialized && firstScanPerformed && coordinator.isComplete(currentFilePaths.size)) {
                    val allEntries = coordinator.getAggregatedInitialEntries()
                    logger.info { "Initial remote directory load complete: ${allEntries.size} entries" }
                    send(LogUpdate.Initial(allEntries).right())
                    initialized = true
                }

                val delayInterval = if (initialized) rescanIntervalMs.milliseconds else 500.milliseconds
                delay(delayInterval)
            }
        } finally {
            pool.close()
        }
    }.flowOn(dispatcher)
}
