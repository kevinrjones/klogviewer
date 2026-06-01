package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.smithy.kotlin.runtime.content.decodeToString
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

class S3LogSource(
    private val config: S3Config,
    private val parser: LogParser,
    private val s3ClientProvider: S3ClientProvider = S3ClientProvider(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pollingInterval: Duration = 5.seconds
) : LogSource {

    override fun observeLogs(path: LogFilePath, parser: LogParser?): Flow<Either<LogFailure, LogUpdate>> = flow<Either<LogFailure, LogUpdate>> {
        val effectiveParser = parser ?: this@S3LogSource.parser
        val multilineProcessor = if (effectiveParser is TemplateLogParser) {
            MultilineProcessor(effectiveParser.template)
        } else null

        val sourceId = "s3://${config.bucket}/${path.value.removePrefix("/")}"
        logger.info { "Started observing S3 log object: $sourceId using ${effectiveParser::class.simpleName}" }

        val client = try {
            s3ClientProvider.createClient(config)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(LogFailure.FileError("Failed to create S3 client: ${e.message}", sourceId).left())
            return@flow
        }

        try {
            var lastSize = 0L
            var isInitial = true
            val initialEntries = mutableListOf<LogEntry>()
            var failedPollCount = 0
            var pollAttempt = 0L

            while (currentCoroutineContext().isActive) {
                pollAttempt += 1
                logger.debug {
                    "Starting S3 poll #$pollAttempt for $sourceId (lastSize=$lastSize, isInitial=$isInitial)"
                }

                try {
                    val keyVal = path.value.removePrefix("/")
                    val headResponse = try {
                        client.headObject(HeadObjectRequest {
                            bucket = config.bucket
                            key = keyVal
                        })
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        val message = "S3 object not found or inaccessible: $sourceId. Error: ${e.message}"
                        logger.warn(e) {
                            "S3 poll #$pollAttempt headObject failed for $sourceId (consecutiveFailures=${failedPollCount + 1})"
                        }
                        emit(LogFailure.FileError(message, sourceId).left())
                        failedPollCount += 1
                        delay(pollingInterval)
                        continue
                    }

                    if (failedPollCount > 0) {
                        logger.info {
                            "S3 polling recovered for $sourceId after $failedPollCount failed attempt(s)"
                        }
                        failedPollCount = 0
                    }

                    val currentSize = headResponse.contentLength ?: 0L
                    val sizeDelta = currentSize - lastSize

                    logger.debug {
                        "S3 poll #$pollAttempt metadata for $sourceId (currentSize=$currentSize, lastSize=$lastSize, sizeDelta=$sizeDelta)"
                    }

                    if (currentSize > lastSize) {
                        val bytesToRead = currentSize - lastSize
                        val rangeVal = "bytes=$lastSize-${currentSize - 1}"
                        logger.info {
                            "S3 poll #$pollAttempt found new data for $sourceId (bytesToRead=$bytesToRead, range=$rangeVal)"
                        }
                        val request = GetObjectRequest {
                            bucket = config.bucket
                            key = keyVal
                            range = rangeVal
                        }
                        
                        val appendedEntries = mutableListOf<LogEntry>()
                        var returnedByteCount = 0
                        var nonBlankLineCount = 0
                        var parsedEntryCount = 0

                        client.getObject(request) { response ->
                            val content = response.body?.decodeToString() ?: ""
                            val lines = content.lines().filter { it.isNotEmpty() }
                            returnedByteCount = content.toByteArray().size
                            nonBlankLineCount = lines.size
                            
                            lines.forEach { line ->
                                val entry = if (multilineProcessor != null) {
                                    multilineProcessor.process(line)
                                } else {
                                    effectiveParser.parse(line).getOrNull()
                                }
                                
                                entry?.let { e ->
                                    val entryWithSource = e.copy(sourceId = sourceId)
                                    if (isInitial) {
                                        initialEntries.add(entryWithSource)
                                    } else {
                                        appendedEntries.add(entryWithSource)
                                    }
                                    parsedEntryCount += 1
                                }
                            }
                        }

                        logger.info {
                            "S3 poll #$pollAttempt fetch result for $sourceId (returnedBytes=$returnedByteCount, nonBlankLines=$nonBlankLineCount, parsedEntries=$parsedEntryCount, initialBatch=$isInitial)"
                        }
                        
                        if (!isInitial && appendedEntries.isNotEmpty()) {
                            logger.info {
                                "S3 poll #$pollAttempt emitting ${appendedEntries.size} appended entries for $sourceId"
                            }
                            emit(LogUpdate.Appended(appendedEntries.toList()).right())
                        } else if (!isInitial) {
                            logger.debug {
                                "S3 poll #$pollAttempt produced no appended entries for $sourceId"
                            }
                        }
                        lastSize = currentSize
                    } else if (currentSize < lastSize) {
                        logger.info {
                            "S3 poll #$pollAttempt detected truncation/replacement for $sourceId (currentSize=$currentSize, lastSize=$lastSize). Resetting lastSize to 0."
                        }
                        lastSize = 0
                        continue 
                    } else {
                        logger.debug { "S3 poll #$pollAttempt found no new data for $sourceId" }
                    }

                    if (isInitial) {
                        multilineProcessor?.flush()?.let { e ->
                            initialEntries.add(e.copy(sourceId = sourceId))
                        }
                        logger.info {
                            "S3 poll #$pollAttempt emitting initial batch of ${initialEntries.size} entries for $sourceId"
                        }
                        emit(LogUpdate.Initial(initialEntries.toList()).right())
                        initialEntries.clear()
                        isInitial = false
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error(e) {
                        "Error during S3 poll #$pollAttempt for $sourceId (lastSize=$lastSize, consecutiveFailures=${failedPollCount + 1})"
                    }
                    emit(LogFailure.FileError("Error polling S3 object: ${e.message}", sourceId).left())
                    failedPollCount += 1
                }

                delay(pollingInterval)
            }
        } finally {
            client.close()
        }
    }.flowOn(dispatcher)
}
