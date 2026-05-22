package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.parser.MultilineProcessor
import com.klogviewer.core.parser.TemplateLogParser
import com.klogviewer.core.util.withRetry
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class SftpLogSource(
    private val config: SftpConfig,
    private val parser: LogParser,
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val existingClient: SSHClient? = null
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = flow {
        val effectiveParser = parser ?: this@SftpLogSource.parser
        val multilineProcessor = if (effectiveParser is TemplateLogParser) {
            MultilineProcessor(effectiveParser.template)
        } else null

        val sourceId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}${path.value}"
        logger.info { "Started observing remote log file: $sourceId using ${effectiveParser::class.simpleName}" }

        val client = if (existingClient != null && existingClient.isConnected && existingClient.isAuthenticated) {
            existingClient
        } else {
            withRetry(maxRetries = 3) {
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
        }
        
        try {
            client.startSession().use { session ->
                val command = session.exec("tail -n +1 -f \"${path.value}\"")
                val errorReader = BufferedReader(InputStreamReader(command.errorStream))
                val inputStream = command.inputStream
                coroutineScope {
                    val parentJob = currentCoroutineContext().job
                    val cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                        try {
                            awaitCancellation()
                        } finally {
                            if (parentJob.isCancelled) {
                                logger.debug { "Cancellation requested for remote log tail: $sourceId. Closing command/session to unblock read." }
                                runCatching { inputStream.close() }
                                    .onFailure { logger.debug(it) { "Ignoring input stream close error during cancellation for $sourceId" } }
                                runCatching { command.close() }
                                    .onFailure { logger.debug(it) { "Ignoring command close error during cancellation for $sourceId" } }
                                runCatching { session.close() }
                                    .onFailure { logger.debug(it) { "Ignoring session close error during cancellation for $sourceId" } }
                            }
                        }
                    }

                    try {
                        inputStream.use { input ->
                            val reader = BufferedReader(InputStreamReader(input))

                        var isInitial = true
                        val initialEntries = mutableListOf<LogEntry>()

                        while (currentCoroutineContext().isActive) {
                            val ready = withContext(dispatcher) { reader.ready() }
                            if (!ready) {
                                if (isInitial) {
                                    multilineProcessor?.flush()?.let {
                                        initialEntries.add(it.copy(sourceId = sourceId))
                                    }
                                    emit(LogUpdate.Initial(initialEntries.toList()).right())
                                    initialEntries.clear()
                                    isInitial = false
                                }

                                val exitStatus = withContext(dispatcher) { command.exitStatus }
                                if (exitStatus != null) {
                                    if (exitStatus != 0) {
                                        val error = withContext(dispatcher) {
                                            if (errorReader.ready()) errorReader.readLine() else null
                                        } ?: "Remote process exited with status $exitStatus"
                                        emit(LogFailure.FileError("Remote error: $error", sourceId = sourceId).left())
                                    } else if (isInitial) {
                                        emit(LogUpdate.Initial(emptyList<LogEntry>()).right())
                                    }
                                    break
                                }

                                delay(200.milliseconds)
                                continue
                            }

                            val line = withContext(dispatcher) { reader.readLine() }
                            if (line == null) {
                                logger.debug { "Remote stream ended for $sourceId" }
                                break
                            }

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
                        }
                        }
                    } finally {
                        cancellationWatcher.cancelAndJoin()
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException || e.cause is InterruptedException) throw e
            logger.error(e) { "Error tailing remote log file: $sourceId" }
            emit(LogFailure.FileError("Error tailing remote log file: ${e.message}", sourceId = sourceId, cause = e).left())
        } finally {
            if (existingClient == null) {
                withContext(NonCancellable + dispatcher) {
                    val disconnected = withTimeoutOrNull(2.seconds) {
                        try {
                            client.disconnect()
                            client.close()
                            true
                        } catch (e: Exception) {
                            logger.warn { "Error disconnecting SSH client: ${e.message}" }
                            true
                        }
                    } ?: false

                    if (!disconnected) {
                        logger.warn { "Timed out while disconnecting SSH client for $sourceId" }
                    }
                }
            }
        }
    }.flowOn(dispatcher)
}
