package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.LogFilePath
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class S3DirectoryLogSource(
    private val config: S3Config,
    private val remoteFileSystem: RemoteFileSystem,
    private val rescanIntervalMs: Long = 10000,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logSourceFactory: (S3Config) -> LogSource = { cfg ->
        S3LogSource(cfg, com.klogviewer.core.parser.SimpleLogParser())
    }
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = channelFlow<Either<LogFailure, LogUpdate>> {
        logger.info { "Started observing S3 prefix: ${path.value} in bucket ${config.bucket}" }

        val coordinator = LogInitialLoadCoordinator()
        val observer = S3DirectoryFileObserver(
            config = config,
            logSourceFactory = logSourceFactory,
            coordinator = coordinator,
            onUpdate = { update -> send(update.right()) }
        )

        try {
            val effectivePrefix = if (path.value.endsWith("/") || path.value.isEmpty()) path.value else "${path.value}/"
            val directorySourceId = "s3://${config.bucket}/${effectivePrefix.removePrefix("/")}"
            var firstScanPerformed = false
            var currentFilePaths = emptyList<String>()
            var initialized = false
            var failedScanCount = 0

            while (isActive) {
                if (!firstScanPerformed || initialized) {
                    val result = try {
                        remoteFileSystem.listS3Objects(config, effectivePrefix)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.error(e) { "Transient exception scanning S3 prefix: $directorySourceId" }
                        send(LogFailure.FileError("Error scanning S3 prefix: ${e.message}", directorySourceId).left())
                        failedScanCount += 1
                        delay(rescanIntervalMs.milliseconds)
                        continue
                    }
                    firstScanPerformed = true

                    result.fold(
                        { failure ->
                            logger.error { "Error scanning S3 prefix: $failure" }
                            send(failure.left())
                            failedScanCount += 1
                        },
                        { discoveredObjects ->
                            if (failedScanCount > 0) {
                                logger.info {
                                    "S3 prefix scan recovered for $directorySourceId after $failedScanCount failed attempt(s)"
                                }
                                failedScanCount = 0
                            }
                            currentFilePaths = discoveredObjects.filter { !it.isDirectory }.map { it.path }
                            observer.updateFiles(currentFilePaths, parser, this)
                        }
                    )
                }

                if (!initialized && coordinator.isComplete(currentFilePaths.size)) {
                    val allEntries = coordinator.getAggregatedInitialEntries()
                    logger.info { "Initial S3 prefix load complete: ${allEntries.size} entries" }
                    send(LogUpdate.Initial(allEntries).right())
                    initialized = true
                    observer.setInitialized()
                }

                val delayInterval = if (initialized) rescanIntervalMs.milliseconds else 1000.milliseconds
                delay(delayInterval)
            }
        } finally {
            observer.cancelAll()
        }
    }.flowOn(dispatcher)
}
