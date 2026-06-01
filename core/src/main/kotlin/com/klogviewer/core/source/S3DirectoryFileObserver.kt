package com.klogviewer.core.source

import com.klogviewer.domain.model.LogFilePath
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class S3DirectoryFileObserver(
    private val config: S3Config,
    private val logSourceFactory: (S3Config) -> LogSource,
    private val coordinator: LogInitialLoadCoordinator,
    private val onUpdate: suspend (LogUpdate) -> Unit
) {
    private val activeSources = mutableMapOf<String, Job>()
    private var initialized = false
    private val sourceIdBase = "s3://${config.bucket}"

    fun setInitialized() {
        initialized = true
    }

    suspend fun updateFiles(discoveredFiles: List<String>, parser: LogParser?, scope: CoroutineScope) {
        pruneInactiveSources()

        val newFiles = discoveredFiles.filter { !activeSources.containsKey(it) }
        val removedFiles = activeSources.keys.filter { !discoveredFiles.contains(it) }

        if (removedFiles.isNotEmpty()) {
            logger.info { "Removing ${removedFiles.size} S3 objects" }
            for (file in removedFiles) {
                activeSources[file]?.cancel()
                activeSources.remove(file)
                onUpdate(LogUpdate.SourceMissing("$sourceIdBase/${file.removePrefix("/")}"))
            }
        }

        if (newFiles.isNotEmpty()) {
            logger.info { "Discovered ${newFiles.size} new S3 objects: $newFiles" }
            newFiles.forEachIndexed { index, file ->
                activeSources[file] = scope.launch {
                    if (index > 0) delay((index * 200L).milliseconds)
                    observeFile(file, parser)
                }
            }
        }
    }

    private fun pruneInactiveSources() {
        val completedFiles = activeSources
            .filterValues { !it.isActive }
            .keys
            .toList()

        if (completedFiles.isNotEmpty()) {
            logger.info { "Pruning ${completedFiles.size} inactive S3 object observer(s): $completedFiles" }
            completedFiles.forEach { activeSources.remove(it) }
        }
    }

    private suspend fun observeFile(file: String, parser: LogParser?) {
        try {
            val s3Source = logSourceFactory(config)
            val path = if (file.startsWith("/")) file else "/$file"
            s3Source.observeLogs(LogFilePath(path), parser).collect { fileResult ->
                fileResult.fold(
                    { failure ->
                        logger.error { "Error observing S3 object $file: $failure" }
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
            logger.error(e) { "Failed to observe S3 object $file" }
            if (!initialized) coordinator.onFileFailedInitial(file)
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
