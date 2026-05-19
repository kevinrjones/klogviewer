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
import kotlinx.coroutines.flow.*
import java.io.File

private val logger = KotlinLogging.logger {}

class DirectoryLogSource(
    private val fileLogSource: LogSource,
    private val heuristicProbe: HeuristicProbe,
    private val scanner: DirectoryScanner = DirectoryScanner(),
    private val rescanIntervalMs: Long = 5000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow {
        val root = File(path.value)
        if (!root.exists() || !root.isDirectory) {
            send(LogFailure.FileError("Not a directory: ${path.value}").left())
            return@channelFlow
        }

        logger.info { "Started observing directory: ${path.value}" }

        val activeSources = mutableMapOf<String, Job>()
        val currentEntries = mutableMapOf<String, List<LogEntry>>()
        var initialized = false

        while (isActive) {
            val discoveredFiles = scanner.scan(path.value)
            val newFiles = discoveredFiles.filter { !activeSources.containsKey(it) }
            val removedFiles = activeSources.keys.filter { !discoveredFiles.contains(it) }

            if (removedFiles.isNotEmpty()) {
                logger.info { "Removing ${removedFiles.size} files from directory source" }
                for (file in removedFiles) {
                    activeSources[file]?.cancel()
                    activeSources.remove(file)
                    currentEntries.remove(file)
                    send(LogUpdate.SourceMissing(file).right())
                }
                // For now, we don't emit a reset because we don't want to clear EVERYTHING.
                // But in a real app, we might need a way to remove specific entries.
            }

            if (newFiles.isNotEmpty()) {
                logger.info { "Discovered ${newFiles.size} new files in directory: $newFiles" }
                for (file in newFiles) {
                    activeSources[file] = launch {
                        val sampleLines = try {
                            File(file).useLines { it.take(50).toList() }
                        } catch (e: Exception) {
                            emptyList<String>()
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
                                                send(LogUpdate.Appended(update.entries).right())
                                            }
                                        }
                                        is LogUpdate.Appended -> {
                                            logger.debug { "Appending ${update.entries.size} entries from $file" }
                                            send(update.right())
                                        }
                                        LogUpdate.Reset -> {
                                            logger.warn { "File $file was reset" }
                                            // Handle reset?
                                        }
                                        is LogUpdate.SourceMissing -> {
                                            send(update.right())
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (!initialized && discoveredFiles.isNotEmpty() && currentEntries.size == discoveredFiles.size) {
                val allEntries = currentEntries.values.flatten().sortedBy { it.timestamp.value }
                logger.info { "Initial directory load complete: ${allEntries.size} entries from ${activeSources.size} files" }
                send(LogUpdate.Initial(allEntries).right())
                initialized = true
            } else if (!initialized && discoveredFiles.isEmpty()) {
                send(LogUpdate.Initial(emptyList()).right())
                initialized = true
            }

            delay(rescanIntervalMs)
        }
    }.flowOn(dispatcher)
}
