package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.schmizz.sshj.SSHClient
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class SftpDirectoryLogSource(
    private val config: SftpConfig,
    private val remoteFileSystem: RemoteFileSystem,
    private val rescanIntervalMs: Long = 5000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sshService: SshService = SshService(),
    private val logSourceFactory: (SftpConfig, SSHClient?) -> LogSource = { cfg, client ->
        SftpLogSource(cfg, com.klogviewer.core.parser.SimpleLogParser(), sshService = sshService, existingClient = client)
    }
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow<Either<LogFailure, LogUpdate>> {
        logger.info { "Started observing remote directory: ${path.value} on ${config.host.value}" }

        val pool = SshClientPool(config, sshService)
        val coordinator = LogInitialLoadCoordinator()
        val observer = RemoteDirectoryFileObserver(
            config = config,
            pool = pool,
            logSourceFactory = logSourceFactory,
            coordinator = coordinator,
            onUpdate = { update -> send(update.right()) }
        )

        try {
            val sourceIdBase = "sftp://${config.username.value}@${config.host.value}:${config.port.value}"
            val directorySourceId = "$sourceIdBase${path.value}"
            var firstScanPerformed = false
            var currentFilePaths = emptyList<String>()
            var initialized = false

            while (isActive) {
                if (!firstScanPerformed || initialized) {
                    val result = remoteFileSystem.listFiles(config, path.value)
                    firstScanPerformed = true

                    result.fold(
                        { failure ->
                            logger.error { "Error scanning remote directory: $failure" }
                            val failureWithSource = when (failure) {
                                is LogFailure.FileError -> failure.copy(sourceId = directorySourceId)
                                is LogFailure.ParsingError -> failure.copy(sourceId = directorySourceId)
                            }
                            send(failureWithSource.left())
                        },
                        { discoveredFiles ->
                            currentFilePaths = discoveredFiles.filter { !it.isDirectory }.map { it.path }
                            observer.updateFiles(currentFilePaths, parser, this)
                        }
                    )
                }

                if (!initialized && firstScanPerformed && coordinator.isComplete(currentFilePaths.size)) {
                    val allEntries = coordinator.getAggregatedInitialEntries()
                    logger.info { "Initial remote directory load complete: ${allEntries.size} entries" }
                    send(LogUpdate.Initial(allEntries).right())
                    initialized = true
                    observer.setInitialized()
                }

                val delayInterval = if (initialized) rescanIntervalMs.milliseconds else 500.milliseconds
                delay(delayInterval)
            }
        } finally {
            observer.cancelAll()
            pool.close()
        }
    }.flowOn(dispatcher)
}
