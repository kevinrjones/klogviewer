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
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class SftpDirectoryLogSource(
    private val config: SftpConfig,
    private val remoteFileSystem: RemoteFileSystem,
    private val rescanIntervalMs: Long = 10000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow {
        logger.info { "Started observing remote directory: ${path.value} on ${config.host.value}" }

        val activeSources = mutableMapOf<String, Job>()
        val currentEntries = mutableMapOf<String, List<LogEntry>>()
        var initialized = false

        val sourceIdBase = "sftp://${config.username.value}@${config.host.value}:${config.port.value}"

        while (isActive) {
            val result = remoteFileSystem.listFiles(config, path.value)
            
            result.fold(
                { failure -> logger.error { "Error scanning remote directory: $failure" } },
                { discoveredFiles ->
                    val filePaths = discoveredFiles.filter { !it.isDirectory }.map { it.path }
                    val newFiles = filePaths.filter { !activeSources.containsKey(it) }
                    val removedFiles = activeSources.keys.filter { !filePaths.contains(it) }

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
                                
                                val sftpSource = SftpLogSource(config, com.klogviewer.core.parser.SimpleLogParser())
                                sftpSource.observeLogs(LogFilePath(file), parser).collect { fileResult ->
                                    fileResult.fold(
                                        { failure -> 
                                            logger.error { "Error observing remote file $file: $failure" }
                                            send(failure.left())
                                        },
                                        { update ->
                                            when (update) {
                                                is LogUpdate.Initial -> {
                                                    currentEntries[file] = update.entries
                                                    if (initialized) {
                                                        send(LogUpdate.Appended(update.entries).right())
                                                    }
                                                }
                                                is LogUpdate.Appended -> send(update.right())
                                                LogUpdate.Reset -> {}
                                                is LogUpdate.SourceMissing -> send(update.right())
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (!initialized && (discoveredFiles.isEmpty() || currentEntries.size >= filePaths.size)) {
                        val allEntries = currentEntries.values.flatten().sortedBy { it.timestamp.value }
                        send(LogUpdate.Initial(allEntries).right())
                        initialized = true
                    }
                }
            )

            delay(rescanIntervalMs)
        }
    }.flowOn(dispatcher)
}
