package com.logviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.core.parser.TemplateLogParser
import com.logviewer.core.parser.MultilineProcessor
import com.logviewer.domain.parser.LogParser
import com.logviewer.domain.model.*
import com.logviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.RandomAccessFile

private val logger = KotlinLogging.logger {}

class FileLogSource(
    private val parser: LogParser,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath, requestedParser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = flow {
        val effectiveParser = requestedParser ?: this@FileLogSource.parser
        val multilineProcessor = if (effectiveParser is TemplateLogParser) {
            MultilineProcessor(effectiveParser.template)
        } else null

        logger.info { "Started observing log file: ${path.value} using ${effectiveParser::class.simpleName} (multiline=${multilineProcessor != null})" }
        val file = File(path.value)
        if (!file.exists()) {
            logger.error { "Log file does not exist: ${path.value}" }
            emit(LogFailure.FileError("File does not exist: ${path.value}").left())
            return@flow
        }

        try {
            // Initial load
            logger.debug { "Performing initial load for ${file.name}" }
            val initialEntries = file.useLines { lines ->
                if (multilineProcessor != null) {
                    val entries = mutableListOf<LogEntry>()
                    lines.forEach { line ->
                        multilineProcessor.process(line)?.let { entries.add(it.copy(sourceId = file.name)) }
                    }
                    multilineProcessor.flush()?.let { entries.add(it.copy(sourceId = file.name)) }
                    entries
                } else {
                    lines.mapNotNull { line ->
                        effectiveParser.parse(line).getOrNull()?.copy(sourceId = file.name)
                    }.toList()
                }
            }
            logger.info { "Initial load completed for ${file.name}: ${initialEntries.size} entries found" }
            emit(LogUpdate.Initial(initialEntries).right())

            // Tailing
            var lastPosition = file.length()
            while (true) {
                delay(1000) // Poll every second
                val currentLength = file.length()
                
                if (currentLength < lastPosition) {
                    // File was truncated
                    logger.warn { "Log file was truncated: ${file.name}. Resetting." }
                    emit(LogUpdate.Reset.right())
                    lastPosition = 0
                }
                
                if (currentLength > lastPosition) {
                    val newEntries = mutableListOf<LogEntry>()
                    RandomAccessFile(file, "r").use { raf ->
                        raf.seek(lastPosition)
                        var line = raf.readLine()
                        while (line != null) {
                            val utf8Line = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                            if (multilineProcessor != null) {
                                multilineProcessor.process(utf8Line)?.let {
                                    newEntries.add(it.copy(sourceId = file.name))
                                }
                            } else {
                                effectiveParser.parse(utf8Line).getOrNull()?.let {
                                    newEntries.add(it.copy(sourceId = file.name))
                                }
                            }
                            line = raf.readLine()
                        }
                        // Note: for tailing, we might want to wait for more lines or flush on timeout
                        // but for now we'll flush at the end of the batch
                        if (multilineProcessor != null) {
                            multilineProcessor.flush()?.let {
                                newEntries.add(it.copy(sourceId = file.name))
                            }
                        }
                        lastPosition = raf.filePointer
                    }
                    if (newEntries.isNotEmpty()) {
                        logger.debug { "Appended ${newEntries.size} new entries from ${file.name}" }
                        emit(LogUpdate.Appended(newEntries).right())
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error tailing log file: ${path.value}" }
            emit(LogFailure.FileError("Error tailing log file: ${e.message}", e).left())
        }
    }.flowOn(dispatcher)
}
