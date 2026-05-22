package com.klogviewer.core.source

import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import net.schmizz.sshj.SSHClient
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class RemoteDirectoryFileObserver(
    private val config: SftpConfig,
    private val pool: SshClientPool,
    private val logSourceFactory: (SftpConfig, SSHClient?) -> LogSource,
    private val coordinator: LogInitialLoadCoordinator,
    private val onUpdate: suspend (LogUpdate) -> Unit
) {
    private val activeSources = mutableMapOf<String, Job>()
    private var initialized = false
    private val sourceIdBase = "sftp://${config.username.value}@${config.host.value}:${config.port.value}"

    fun setInitialized() {
        initialized = true
    }

    suspend fun updateFiles(discoveredFiles: List<String>, parser: LogParser?, scope: CoroutineScope) {
        val newFiles = discoveredFiles.filter { !activeSources.containsKey(it) }
        val removedFiles = activeSources.keys.filter { !discoveredFiles.contains(it) }

        if (removedFiles.isNotEmpty()) {
            logger.info { "Removing ${removedFiles.size} remote files" }
            for (file in removedFiles) {
                activeSources[file]?.cancel()
                activeSources.remove(file)
                onUpdate(LogUpdate.SourceMissing("$sourceIdBase$file"))
            }
        }

        if (newFiles.isNotEmpty()) {
            logger.info { "Discovered ${newFiles.size} new remote files: $newFiles" }
            newFiles.forEachIndexed { index, file ->
                activeSources[file] = scope.launch {
                    if (index > 0) delay((index * 200L).milliseconds)
                    observeFile(file, parser)
                }
            }
        }
    }

    private suspend fun observeFile(file: String, parser: LogParser?) {
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
                        handleUpdate(file, update)
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

    private suspend fun handleUpdate(file: String, update: LogUpdate) {
        when (update) {
            is LogUpdate.Initial -> {
                if (!initialized) {
                    coordinator.onInitialLoad(file, update.entries)
                } else {
                    onUpdate(LogUpdate.Appended(update.entries))
                }
            }
            is LogUpdate.Appended -> {
                if (!initialized) {
                    coordinator.onAppendedDuringInitial(file, update.entries)
                }
                onUpdate(update)
            }
            LogUpdate.Reset -> {}
            is LogUpdate.SourceMissing -> onUpdate(update)
        }
    }

    fun cancelAll() {
        activeSources.values.forEach { it.cancel() }
        activeSources.clear()
    }
}
