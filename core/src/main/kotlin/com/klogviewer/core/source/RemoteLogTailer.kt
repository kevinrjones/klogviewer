package com.klogviewer.core.source

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import net.schmizz.sshj.SSHClient
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class RemoteLogTailer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun tailFile(
        client: SSHClient,
        path: String,
        onLine: suspend (String) -> Unit,
        onReady: suspend () -> Unit,
        onError: suspend (String) -> Unit
    ) {
        client.startSession().use { session ->
            val command = session.exec("tail -n +1 -f \"$path\"")
            val errorReader = BufferedReader(InputStreamReader(command.errorStream))
            val inputStream = command.inputStream
            
            coroutineScope {
                val parentJob = currentCoroutineContext().job
                val cancellationWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        if (parentJob.isCancelled) {
                            runCatching { inputStream.close() }
                            runCatching { command.close() }
                            runCatching { session.close() }
                        }
                    }
                }

                try {
                    inputStream.use { input ->
                        val reader = BufferedReader(InputStreamReader(input))
                        while (currentCoroutineContext().isActive) {
                            val ready = withContext(dispatcher) { reader.ready() }
                            if (!ready) {
                                onReady()
                                val exitStatus = withContext(dispatcher) { command.exitStatus }
                                if (exitStatus != null) {
                                    if (exitStatus != 0) {
                                        val error = withContext(dispatcher) {
                                            if (errorReader.ready()) errorReader.readLine() else null
                                        } ?: "Remote process exited with status $exitStatus"
                                        onError(error)
                                    }
                                    break
                                }
                                delay(200.milliseconds)
                                continue
                            }

                            val line = withContext(dispatcher) { reader.readLine() }
                            if (line == null) break
                            onLine(line)
                        }
                    }
                } finally {
                    cancellationWatcher.cancelAndJoin()
                }
            }
        }
    }
}
