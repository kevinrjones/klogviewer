package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.LogFilePath
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger {}

class MergedLogSource(
    private val sources: List<Triple<LogSource, LogFilePath, LogParser?>>
) {
    fun observeMerged(): Flow<Either<LogFailure, LogUpdate>> = channelFlow {
        logger.info { "Merging ${sources.size} log sources" }
        if (sources.isEmpty()) {
            send(LogUpdate.Initial(emptyList()).right())
            return@channelFlow
        }

        val flows = sources.map { (source, path, parser) -> source.observeLogs(path, parser) }
        
        // Track the current entries from each source for initial merge
        val currentEntries = mutableMapOf<Int, List<com.klogviewer.domain.model.LogEntry>>()
        var initializedCount = 0

        flows.forEachIndexed { index, flow ->
            launch {
                flow.collect { result ->
                    result.fold(
                        ifLeft = { 
                            logger.error { "Error in one of the merged sources: ${it}" }
                            send(it.left()) 
                        },
                        ifRight = { update ->
                            when (update) {
                                is LogUpdate.Initial -> {
                                    currentEntries[index] = update.entries
                                    initializedCount++
                                    if (initializedCount == sources.size) {
                                        val all = currentEntries.values.flatten().sortedBy { it.timestamp.value }
                                        send(LogUpdate.Initial(all).right())
                                    }
                                }
                                is LogUpdate.Appended -> {
                                    // If we are already initialized, we can just send the appends
                                    // Note: This might break chronological order if appends arrive out of sync across files,
                                    // but for a live tail it's usually acceptable to show them as they come.
                                    // A perfect merge would require buffering.
                                    send(LogUpdate.Appended(update.entries).right())
                                }
                                LogUpdate.Reset -> {
                                    send(LogUpdate.Reset.right())
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
}
