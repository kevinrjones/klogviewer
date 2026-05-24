package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.parser.MultilineProcessor
import com.klogviewer.core.parser.TemplateLogParser
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.schmizz.sshj.SSHClient

private val logger = KotlinLogging.logger {}

class SftpLogSource(
    private val config: SftpConfig,
    private val parser: LogParser,
    private val sshService: SshService = SshService(),
    private val tailer: RemoteLogTailer = RemoteLogTailer(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val existingClient: SSHClient? = null
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = flow {
        val effectiveParser = parser ?: this@SftpLogSource.parser
        val multilineProcessor = if (effectiveParser is TemplateLogParser) {
            MultilineProcessor(effectiveParser.template)
        } else null

        val effectivePath = if (path.value.startsWith("/")) path.value else "/${path.value}"
        val sourceId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}${effectivePath}"
        logger.info { "Started observing remote log file: $sourceId using ${effectiveParser::class.simpleName}" }

        val client = if (existingClient != null && existingClient.isConnected && existingClient.isAuthenticated) {
            existingClient
        } else {
            try {
                sshService.connectAndAuthenticate(config)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                emit(e.toLogFailure("Failed to connect to ${config.host.value}", sourceId).left())
                return@flow
            }
        }
        
        try {
            var isInitial = true
            val initialEntries = mutableListOf<LogEntry>()

            tailer.tailFile(
                client = client,
                path = path.value,
                onLine = { line ->
                    val entry = if (multilineProcessor != null) {
                        multilineProcessor.process(line)
                    } else {
                        effectiveParser.parse(line).getOrNull()
                    }

                    entry?.let {
                        val entryWithSource = it.copy(sourceId = sourceId)
                        if (isInitial) {
                            initialEntries.add(entryWithSource)
                        } else {
                            emit(LogUpdate.Appended(listOf(entryWithSource)).right())
                        }
                    }
                },
                onReady = {
                    if (isInitial) {
                        multilineProcessor?.flush()?.let {
                            initialEntries.add(it.copy(sourceId = sourceId))
                        }
                        emit(LogUpdate.Initial(initialEntries.toList()).right())
                        initialEntries.clear()
                        isInitial = false
                    }
                },
                onError = { error ->
                    emit(error.toFileError(sourceId).left())
                }
            )
        } catch (e: Exception) {
            if (e is CancellationException || e.cause is InterruptedException) throw e
            logger.error(e) { "Error tailing remote log file: $sourceId" }
            emit(e.toLogFailure("Error tailing remote log file: ${e.message}", sourceId).left())
        } finally {
            if (existingClient == null) {
                withContext(NonCancellable + dispatcher) {
                    try {
                        client.disconnect()
                        client.close()
                    } catch (e: Exception) {
                        logger.warn { "Error disconnecting SSH client: ${e.message}" }
                    }
                }
            }
        }
    }.flowOn(dispatcher)
}
