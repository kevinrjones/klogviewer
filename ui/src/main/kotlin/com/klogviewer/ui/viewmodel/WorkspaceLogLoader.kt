package com.klogviewer.ui.viewmodel

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.core.parser.*
import com.klogviewer.core.source.DirectoryLogSource
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

    fun filterRedundantPaths(paths: List<String>): List<String> {
        val directories = paths.filter { path ->
            if (path.startsWith("sftp://")) {
                SftpUri.parse(path)?.isDirectory == true
            } else {
                localFileSystem.isDirectory(path)
            }
        }
        
        if (directories.isEmpty()) return paths

        return paths.filter { path ->
            val sftpUri = SftpUri.parse(path)
            val isDir = sftpUri?.isDirectory == true || localFileSystem.isDirectory(path)
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
                } else if (!path.startsWith("sftp://") && !dir.startsWith("sftp://")) {
                    path.startsWith(dir) && path != dir
                } else false
            }
        }
    }

    fun performHeuristicDetection(paths: List<String>, overrideParserName: String?): List<ProbeResult?> {
        return paths.map { path ->
            if (path.startsWith("sftp://") || (localFileSystem.exists(path) && localFileSystem.isDirectory(path))) {
                null
            } else {
                val sampleLines = readSampleLines(path)
                if (overrideParserName != null) {
                    getParserResultByName(overrideParserName, sampleLines)
                } else {
                    heuristicProbe.detect(sampleLines)
                }
            }
        }
    }

    fun createLogFlows(paths: List<String>, results: List<ProbeResult?>): List<Flow<Either<Pair<LogFailure, String>, Pair<LogUpdate, String>>>> {
        return paths.mapIndexed { index, path ->
            val flow = when {
                path.startsWith("sftp://") -> createSftpLogFlow(path)
                localFileSystem.isDirectory(path) -> DirectoryLogSource(logSource, heuristicProbe).observeLogs(LogFilePath(path))
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
            if (sftpUri.isDirectory) {
                logSourceFactory.createSftpDirectorySource(config, remoteFileSystem).observeLogs(LogFilePath(sftpUri.path))
            } else {
                logSourceFactory.createSftpSource(config).observeLogs(LogFilePath(sftpUri.path))
            }
        } else {
            flowOf(LogFailure.FileError("SFTP connection not found for $path", sourceId = path).left())
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

    fun getParserResultByName(name: String, sampleLines: List<String>): ProbeResult {
        return when (name) {
            "JSON" -> {
                val detected = heuristicProbe.detect(sampleLines)
                if (detected.parser is JsonLogParser) detected
                else ProbeResult(JsonLogParser(), "JSON", listOf("Timestamp", "Level", "Content"))
            }
            "logfmt" -> ProbeResult(LogfmtParser(), "logfmt", listOf("Timestamp", "Level", "Content"))
            "Simple" -> ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
            else -> {
                val template = heuristicProbe.registry.getTemplate(name)
                if (template != null) ProbeResult(TemplateLogParser(template), template.name, template.columns)
                else ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
            }
        }
    }

    private fun readSampleLines(path: String, limit: Int = 50): List<String> {
        return try {
            localFileSystem.readLines(path, limit)
        } catch (e: Exception) {
            logger.warn { "Failed to read sample lines from $path: ${e.message}" }
            emptyList()
        }
    }
}
