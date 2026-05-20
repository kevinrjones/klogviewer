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
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = KotlinLogging.logger {}

class SftpLogSource(
    private val config: SftpConfig,
    private val parser: LogParser,
    private val sshClientProvider: SshClientProvider = DefaultSshClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = flow {
        val effectiveParser = parser ?: this@SftpLogSource.parser
        val multilineProcessor = if (effectiveParser is TemplateLogParser) {
            MultilineProcessor(effectiveParser.template)
        } else null

        val sourceId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}${path.value}"
        logger.info { "Started observing remote log file: $sourceId using ${effectiveParser::class.simpleName}" }

        val client = sshClientProvider.createClient()
        client.addHostKeyVerifier(PromiscuousVerifier())
        
        try {
            withContext(Dispatchers.IO) {
                client.connect(config.host.value, config.port.value)
                
                when (val auth = config.auth) {
                    is SftpAuth.Password -> client.authPassword(config.username.value, auth.password)
                    is SftpAuth.KeyPair -> {
                        val keyProvider = if (!auth.passphrase.isNullOrBlank()) {
                            client.loadKeys(auth.privateKeyPath, auth.passphrase)
                        } else {
                            client.loadKeys(auth.privateKeyPath)
                        }
                        client.authPublickey(config.username.value, keyProvider)
                    }
                }
            }

            client.startSession().use { session ->
                val command = session.exec("tail -n +1 -f \"${path.value}\"")
                val errorReader = BufferedReader(InputStreamReader(command.errorStream))
                
                command.inputStream.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    
                    var isInitial = true
                    val initialEntries = mutableListOf<LogEntry>()
                    
                    while (currentCoroutineContext().isActive) {
                        val line = if (withContext(Dispatchers.IO) { reader.ready() }) {
                            withContext(Dispatchers.IO) { reader.readLine() }
                        } else {
                            // Check if command has exited with error
                            val exitStatus = command.exitStatus
                            if (exitStatus != null && exitStatus != 0) {
                                val error = withContext(Dispatchers.IO) { 
                                    if (errorReader.ready()) errorReader.readLine() else null 
                                } ?: "Remote process exited with status $exitStatus"
                                emit(LogFailure.FileError("Remote error: $error").left())
                                return@use
                            }
                            
                            // If not ready, and we were in initial mode, flush initial entries
                            if (isInitial) {
                                multilineProcessor?.flush()?.let {
                                    initialEntries.add(it.copy(sourceId = sourceId))
                                }
                                emit(LogUpdate.Initial(initialEntries.toList()).right())
                                initialEntries.clear()
                                isInitial = false
                            }
                            
                            // Small delay to avoid tight loop when no data is available
                            delay(100)
                            if (withContext(Dispatchers.IO) { reader.ready() }) {
                                withContext(Dispatchers.IO) { reader.readLine() }
                            } else null
                        } ?: if (!isInitial) break else {
                            // readLine returned null, check for error
                            val exitStatus = command.exitStatus
                            if (exitStatus != null && exitStatus != 0) {
                                val error = withContext(Dispatchers.IO) {
                                    if (errorReader.ready()) errorReader.readLine() else null
                                } ?: "File not found or inaccessible"
                                emit(LogFailure.FileError("Remote file not found or inaccessible: ${path.value} ($error)").left())
                            } else if (isInitial) {
                                // If we are still in initial phase and stream closed, check if anything on stderr
                                val error = withContext(Dispatchers.IO) {
                                    if (errorReader.ready()) errorReader.readLine() else null
                                }
                                if (error != null) {
                                    emit(LogFailure.FileError("Remote error: $error").left())
                                } else {
                                    // Just emit empty initial if we didn't find anything
                                    emit(LogUpdate.Initial(emptyList<LogEntry>()).right())
                                }
                            }
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
            }
        } catch (e: Exception) {
            if (e is CancellationException || e.cause is InterruptedException) throw e
            logger.error(e) { "Error tailing remote log file: $sourceId" }
            emit(LogFailure.FileError("Error tailing remote log file: ${e.message}", e).left())
        } finally {
            withContext(NonCancellable) {
                try {
                    client.disconnect()
                    client.close()
                } catch (e: Exception) {
                    logger.warn { "Error disconnecting SSH client: ${e.message}" }
                }
            }
        }
    }.flowOn(dispatcher)
}
