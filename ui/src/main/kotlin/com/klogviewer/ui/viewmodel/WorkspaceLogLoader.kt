package com.klogviewer.ui.viewmodel

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.parser.*
import com.klogviewer.core.source.DirectoryLogSource
import com.klogviewer.core.source.DirectoryScanner
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.*

class WorkspaceLogLoader(
    private val localFileSystem: LocalFileSystem,
    private val remoteFileSystem: RemoteFileSystem,
    private val logSource: LogSource,
    private val heuristicProbe: HeuristicProbe,
    private val logSourceFactory: LogSourceFactory,
    private val state: StateFlow<KLogViewerState>
) {
    private val logger = KotlinLogging.logger {}
    val sourcePatternResolver = SourcePatternResolver(localFileSystem, heuristicProbe, state)

    fun filterRedundantPaths(paths: List<String>): List<String> {
        val directories = paths.filter { path ->
            when {
                path.startsWith("sftp://") -> SftpUri.parse(path)?.isDirectory == true
                path.startsWith("s3://") -> S3Uri.parse(path)?.isDirectory == true
                else -> localFileSystem.isDirectory(path)
            }
        }
        
        if (directories.isEmpty()) return paths

        return paths.filter { path ->
            val sftpUri = SftpUri.parse(path)
            val s3Uri = S3Uri.parse(path)
            val isDir = sftpUri?.isDirectory == true || s3Uri?.isDirectory == true || localFileSystem.isDirectory(path)
            if (isDir) return@filter true
            
            !directories.any { dir ->
                if (path.startsWith("sftp://") && dir.startsWith("sftp://")) {
                    val dirUri = SftpUri.parse(dir)
                    if (sftpUri != null && dirUri != null) {
                        sftpUri.username == dirUri.username &&
                        sftpUri.host == dirUri.host &&
                        sftpUri.port == dirUri.port &&
                        sftpUri.path.startsWith(dirUri.path) &&
                        sftpUri.path != dirUri.path
                    } else false
                } else if (path.startsWith("s3://") && dir.startsWith("s3://")) {
                    val dirUri = S3Uri.parse(dir)
                    if (s3Uri != null && dirUri != null) {
                        s3Uri.bucket == dirUri.bucket &&
                        s3Uri.key.startsWith(dirUri.key) &&
                        s3Uri.key != dirUri.key
                    } else false
                } else if (!path.isRemoteUri() && !dir.isRemoteUri()) {
                    path.startsWith(dir) && path != dir
                } else false
            }
        }
    }

    fun performHeuristicDetection(paths: List<String>, overrideParserName: String?): List<ProbeResult?> {
        return paths.map { path ->
            if (path.isRemoteUri()) {
                null
            } else {
                val sampleLines = readSampleLines(path)
                if (sampleLines.isEmpty()) {
                    null
                } else {
                    sourcePatternResolver.resolveParserForSource(
                        path,
                        sampleLines,
                        overrideParserName,
                        ::getParserResultByName
                    )
                }
            }
        }
    }

    fun createLogFlows(paths: List<String>, results: List<ProbeResult?>): List<Flow<Either<Pair<LogFailure, String>, Pair<LogUpdate, String>>>> {
        return paths.mapIndexed { index, path ->
            val flow = when {
                path.startsWith("sftp://") -> createSftpLogFlow(path)
                path.startsWith("s3://") -> createS3LogFlow(path)
                localFileSystem.isDirectory(path) -> DirectoryLogSource(logSource, heuristicProbe).observeLogs(
                    LogFilePath(path),
                    results.getOrNull(index)?.parser
                )
                else -> logSource.observeLogs(LogFilePath(path), results[index]?.parser)
            }
            flow.map { result ->
                result.fold(
                    { failure -> (failure to path).left() },
                    { update -> (update to path).right() }
                )
            }
        }
    }

    fun createSftpLogFlow(path: String): Flow<Either<LogFailure, LogUpdate>> {
        val config = findSftpConfig(path)
        val sftpUri = SftpUri.parse(path)
        return if (config != null && sftpUri != null) {
            flow {
                val isDir = sftpUri.isDirectory || remoteFileSystem.isSftpDirectory(config, sftpUri.path)
                val source = if (isDir) {
                    logSourceFactory.createSftpDirectorySource(config, remoteFileSystem)
                } else {
                    logSourceFactory.createSftpSource(config)
                }
                emitAll(source.observeLogs(LogFilePath(sftpUri.path)))
            }
        } else {
            flowOf(LogFailure.FileError("SFTP connection not found for $path", sourceId = path).left())
        }
    }

    fun createS3LogFlow(path: String): Flow<Either<LogFailure, LogUpdate>> {
        val config = findS3Config(path)
        val s3Uri = S3Uri.parse(path)
        return if (config != null && s3Uri != null) {
            flow {
                val isDir = s3Uri.isDirectory || remoteFileSystem.isS3Directory(config, s3Uri.key)
                val source = if (isDir) {
                    logSourceFactory.createS3DirectorySource(config, remoteFileSystem)
                } else {
                    logSourceFactory.createS3Source(config)
                }
                emitAll(source.observeLogs(LogFilePath(s3Uri.key)))
            }
        } else {
            flowOf(LogFailure.FileError("S3 connection not found for $path", sourceId = path).left())
        }
    }

    fun findSftpConfig(uri: String): SftpConfig? {
        val sftpUri = SftpUri.parse(uri) ?: return null
        return state.value.sftpConnections.find {
            it.username.value == sftpUri.username &&
            it.host.value == sftpUri.host &&
            it.port.value == sftpUri.port
        }
    }

    fun findS3Config(uri: String): S3Config? {
        val s3Uri = S3Uri.parse(uri) ?: return null
        return state.value.s3Connections.find {
            it.bucket == s3Uri.bucket
        }
    }

    fun getParserResultByName(name: String, sampleLines: List<String>): ProbeResult {
        return when (name) {
            "JSON" -> {
                val detected = heuristicProbe.detect(sampleLines)
                if (detected.parser is JsonLogParser) detected
                else ProbeResult(JsonLogParser(), "JSON", listOf("Timestamp", "Level", "Content"))
            }
            "logfmt" -> ProbeResult(LogfmtParser(), "logfmt", listOf("Timestamp", "Level", "Content"))
            "Simple" -> ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
            "Auto" -> heuristicProbe.detect(sampleLines)
            else -> {
                val template = heuristicProbe.registry.getTemplate(name)
                if (template != null) {
                    ProbeResult(TemplateLogParser(template), template.name, template.columns)
                } else {
                    val matchingMapping = state.value.directoryPatternMappings.values.find {
                        it.patternDraft.name == name
                    }
                    if (matchingMapping != null) {
                        val compiled = PatternDraftCompiler().compile(matchingMapping.patternDraft)
                        heuristicProbe.registry.register(compiled.template)
                        ProbeResult(
                            parser = TemplateLogParser(compiled.template),
                            parserName = compiled.template.name,
                            columns = compiled.template.columns
                        )
                    } else {
                        ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
                    }
                }
            }
        }
    }

    fun readSampleLines(path: String, limit: Int = 50): List<String> {
        return try {
            val targetPath = resolveSampleFile(localFileSystem, path) ?: return emptyList()
            localFileSystem.readLines(targetPath, limit)
        } catch (e: Exception) {
            logger.warn { "Failed to read sample lines from $path: ${e.message}" }
            emptyList()
        }
    }

    fun readResampledLines(path: String, limitPerSection: Int = 10): List<String> {
        return try {
            val targetPath = resolveSampleFile(localFileSystem, path)
            if (targetPath == null) {
                emptyList()
            } else {
                val totalLines = localFileSystem.readLines(targetPath, RESAMPLE_LINE_SCAN_LIMIT)
                sampleResampledSections(totalLines, limitPerSection)
            }
        } catch (e: Exception) {
            logger.warn { "Failed to resample lines from $path: ${e.message}" }
            emptyList()
        }
    }

    private fun String.isRemoteUri(): Boolean =
        startsWith("sftp://") || startsWith("s3://")
}

private const val RESAMPLE_LINE_SCAN_LIMIT = 1000
private const val RESAMPLE_SECTION_MULTIPLIER = 3

private fun resolveSampleFile(localFileSystem: LocalFileSystem, path: String): String? {
    if (!localFileSystem.exists(path)) return null
    return if (!localFileSystem.isDirectory(path)) {
        path
    } else {
        val files = localFileSystem.listFiles(path, listOf("*.log", "*.txt")).ifEmpty {
            localFileSystem.listFiles(path, listOf("*"))
        }.filter { !localFileSystem.isDirectory(it) }
        files.firstOrNull()
    }
}

private fun sampleResampledSections(totalLines: List<String>, limitPerSection: Int): List<String> {
    if (totalLines.isEmpty()) return emptyList()
    return if (totalLines.size <= limitPerSection * RESAMPLE_SECTION_MULTIPLIER) {
        aggregateMultilineLines(totalLines)
    } else {
        val head = totalLines.take(limitPerSection)
        val midStart = (totalLines.size / 2 - limitPerSection / 2).coerceAtLeast(limitPerSection)
        val mid = totalLines.drop(midStart).take(limitPerSection)
        val tail = totalLines.takeLast(limitPerSection)
        aggregateMultilineLines((head + mid + tail).distinct())
    }
}

private fun aggregateMultilineLines(lines: List<String>): List<String> {
    if (lines.isEmpty()) return emptyList()
    val aggregated = mutableListOf<String>()
    var current: StringBuilder? = null

    for (line in lines) {
        val isContinuation = line.startsWith("\t") ||
            line.startsWith("   ") ||
            line.trimStart().startsWith("at ") ||
            line.trimStart().startsWith("Caused by:") ||
            line.trimStart().startsWith("...")

        if (isContinuation && current != null) {
            current.append("\n").append(line)
        } else {
            if (current != null) {
                aggregated.add(current.toString())
            }
            current = StringBuilder(line)
        }
    }
    if (current != null) {
        aggregated.add(current.toString())
    }
    return aggregated
}
