package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class DirectoryLogSource(
    private val fileLogSource: LogSource,
    private val heuristicProbe: HeuristicProbe,
    private val scanner: DirectoryScanner = DirectoryScanner(),
    private val rescanIntervalMs: Long = 5000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow<Either<LogFailure, LogUpdate>> {
        val root = File(path.value)
        if (!validateDirectory(root, path)) return@channelFlow

        logger.info { "Started observing directory: ${path.value}" }

        val activeSources = mutableMapOf<String, Job>()
        val currentEntries = mutableMapOf<String, List<LogEntry>>()
        var initialized = false

        while (isActive) {
            val discoveredFiles = scanDirectory(path, this)
            
            handleRemovedFiles(discoveredFiles, activeSources, currentEntries, this)
            handleNewFiles(discoveredFiles, activeSources, currentEntries, parser, initialized, this)

            if (!initialized) {
                initialized = checkInitialLoad(discoveredFiles, activeSources, currentEntries, this)
            }

            delay(rescanIntervalMs.milliseconds)
        }
    }.flowOn(dispatcher)

    private suspend fun ProducerScope<Either<LogFailure, LogUpdate>>.validateDirectory(root: File, path: LogFilePath): Boolean {
        if (!root.exists() || !root.isDirectory) {
            send(LogFailure.FileError("Not a directory: ${path.value}", sourceId = path.value).left())
            return false
        }
        return true
    }

    private suspend fun scanDirectory(path: LogFilePath, scope: ProducerScope<Either<LogFailure, LogUpdate>>): List<String> {
        return try {
            scanner.scan(path.value)
        } catch (e: Exception) {
            logger.error(e) { "Error scanning directory: ${path.value}" }
            scope.send(LogFailure.FileError("Error scanning directory: ${e.message}", sourceId = path.value, cause = e).left())
            emptyList()
        }
    }

    private suspend fun handleRemovedFiles(
        discoveredFiles: List<String>,
        activeSources: MutableMap<String, Job>,
        currentEntries: MutableMap<String, List<LogEntry>>,
        scope: ProducerScope<Either<LogFailure, LogUpdate>>
    ) {
        val removedFiles = activeSources.keys.filter { !discoveredFiles.contains(it) }
        if (removedFiles.isNotEmpty()) {
            logger.info { "Removing ${removedFiles.size} files from directory source" }
            for (file in removedFiles) {
                activeSources[file]?.cancel()
                activeSources.remove(file)
                currentEntries.remove(file)
                scope.send(LogUpdate.SourceMissing(file).right())
            }
        }
    }

    private fun handleNewFiles(
        discoveredFiles: List<String>,
        activeSources: MutableMap<String, Job>,
        currentEntries: MutableMap<String, List<LogEntry>>,
        parser: LogParser?,
        initialized: Boolean,
        scope: ProducerScope<Either<LogFailure, LogUpdate>>
    ) {
        val newFiles = discoveredFiles.filter { !activeSources.containsKey(it) }
        if (newFiles.isNotEmpty()) {
            logger.info { "Discovered ${newFiles.size} new files in directory: $newFiles" }
            for (file in newFiles) {
                activeSources[file] = scope.launch {
                    observeFile(file, parser, currentEntries, initialized, scope)
                }
            }
        }
    }

    private suspend fun observeFile(
        file: String,
        parser: LogParser?,
        currentEntries: MutableMap<String, List<LogEntry>>,
        initialized: Boolean,
        scope: ProducerScope<Either<LogFailure, LogUpdate>>
    ) {
        val sampleLines = try {
            File(file).useLines { it.take(50).toList() }
        } catch (_: Exception) {
            emptyList()
        }
        val effectiveParser = parser ?: heuristicProbe.detect(sampleLines).parser

        fileLogSource.observeLogs(LogFilePath(file), effectiveParser).collect { result ->
            result.fold(
                ifLeft = { logger.error { "Error observing $file: $it" } },
                ifRight = { update ->
                    when (update) {
                        is LogUpdate.Initial -> {
                            currentEntries[file] = update.entries
                            if (initialized) {
                                logger.debug { "Adding ${update.entries.size} initial entries from new file $file" }
                                scope.send(LogUpdate.Appended(update.entries).right())
                            }
                        }
                        is LogUpdate.Appended -> {
                            logger.debug { "Appending ${update.entries.size} entries from $file" }
                            scope.send(update.right())
                        }
                        LogUpdate.Reset -> {
                            logger.warn { "File $file was reset" }
                        }
                        is LogUpdate.SourceMissing -> {
                            scope.send(update.right())
                        }
                    }
                }
            )
        }
    }

    private suspend fun checkInitialLoad(
        discoveredFiles: List<String>,
        activeSources: Map<String, Job>,
        currentEntries: Map<String, List<LogEntry>>,
        scope: ProducerScope<Either<LogFailure, LogUpdate>>
    ): Boolean {
        if (discoveredFiles.isNotEmpty() && currentEntries.size == discoveredFiles.size) {
            val allEntries = currentEntries.values.flatten().sortedBy { it.timestamp.value }
            logger.info { "Initial directory load complete: ${allEntries.size} entries from ${activeSources.size} files" }
            scope.send(LogUpdate.Initial(allEntries).right())
            return true
        } else if (discoveredFiles.isEmpty()) {
            scope.send(LogUpdate.Initial(emptyList()).right())
            return true
        }
        return false
    }
}
