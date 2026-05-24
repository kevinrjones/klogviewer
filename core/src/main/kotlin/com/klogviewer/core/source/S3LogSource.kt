package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import aws.sdk.kotlin.services.s3.S3Client
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

        val sourceId = "s3://${config.bucket}${path.value}"
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

            while (currentCoroutineContext().isActive) {
                try {
                    val keyVal = path.value.removePrefix("/")
                    val headResponse = try {
                        client.headObject(HeadObjectRequest {
                            bucket = config.bucket
                            key = keyVal
                        })
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logger.warn { "S3 object not found or inaccessible: $sourceId. Error: ${e.message}" }
                        null
                    }

                    val currentSize = headResponse?.contentLength ?: 0L

                    if (currentSize > lastSize) {
                        val rangeVal = "bytes=$lastSize-${currentSize - 1}"
                        val request = GetObjectRequest {
                            bucket = config.bucket
                            key = keyVal
                            range = rangeVal
                        }
                        
                        val appendedEntries = mutableListOf<LogEntry>()
                        client.getObject(request) { response ->
                            val content = response.body?.decodeToString() ?: ""
                            val lines = content.lines().filter { it.isNotEmpty() }
                            
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
                                }
                            }
                        }
                        
                        if (!isInitial && appendedEntries.isNotEmpty()) {
                            emit(LogUpdate.Appended(appendedEntries.toList()).right())
                        }
                        lastSize = currentSize
                    } else if (currentSize < lastSize) {
                        logger.info { "S3 object truncated or replaced: $sourceId. Resetting lastSize to 0." }
                        lastSize = 0
                        continue 
                    }

                    if (isInitial && headResponse != null) {
                        multilineProcessor?.flush()?.let { e ->
                            initialEntries.add(e.copy(sourceId = sourceId))
                        }
                        emit(LogUpdate.Initial(initialEntries.toList()).right())
                        initialEntries.clear()
                        isInitial = false
                    }

                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    logger.error(e) { "Error polling S3 object: $sourceId" }
                    emit(LogFailure.FileError("Error polling S3 object: ${e.message}", sourceId).left())
                }

                delay(pollingInterval)
            }
        } finally {
            client.close()
        }
    }.flowOn(dispatcher)
}
