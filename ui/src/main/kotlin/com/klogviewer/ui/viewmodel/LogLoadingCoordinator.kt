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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LogLoadingCoordinator(
    private val localFileSystem: LocalFileSystem,
    private val remoteFileSystem: RemoteFileSystem,
    private val logSource: LogSource,
    private val heuristicProbe: HeuristicProbe,
    private val logSourceFactory: LogSourceFactory,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<KLogViewerState>,
    private val onSavePreferences: () -> Unit,
    private val onHandleLogUpdate: (String, LogUpdate, String?) -> Unit,
    private val onShowError: suspend (String) -> Unit
) {
    private val logger = KotlinLogging.logger {}
    private val logJobs = mutableMapOf<String, Job>()

    private val workspaceLogLoader = WorkspaceLogLoader(
        localFileSystem = localFileSystem,
        remoteFileSystem = remoteFileSystem,
        logSource = logSource,
        heuristicProbe = heuristicProbe,
        logSourceFactory = logSourceFactory,
        state = state
    )

    fun cancelAll() {
        logJobs.values.forEach { it.cancel() }
        logJobs.clear()
    }

    fun cancelWindowJob(windowId: String) {
        logJobs[windowId]?.cancel()
        logJobs.remove(windowId)
    }

    fun loadFilesIntoWindow(windowId: String, paths: List<String>, overrideParserName: String? = null) {
        val filteredPaths = workspaceLogLoader.filterRedundantPaths(paths)
        if (handleSingleSftpPath(windowId, filteredPaths, overrideParserName)) return

        val oldJob = logJobs[windowId]
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            updateWindowStateForLoading(windowId, filteredPaths)
            onSavePreferences()
            
            val results = workspaceLogLoader.performHeuristicDetection(filteredPaths, overrideParserName)
            updateWindowStateWithParserResults(windowId, results, overrideParserName)
            
            val flows = workspaceLogLoader.createLogFlows(filteredPaths, results)
            val flow = if (flows.size == 1) flows[0] else flows.merge()
            
            flow.collect { result ->
                result.fold(
                    ifLeft = { pair -> 
                        val (failure, path) = pair
                        handleLogLoadingFailure(windowId, path, failure) 
                    },
                    ifRight = { pair -> 
                        val (update, sourceId) = pair
                        onHandleLogUpdate(windowId, update, sourceId) 
                    }
                )
            }
        }
    }

    fun connectSftp(windowId: String, name: String, host: String, port: Int, user: String, auth: SftpAuth, path: String, parserName: String? = null) {
        val config = SftpConfig(name, Host(host), Port(port), Username(user), auth, path)
        val sftpSource = logSourceFactory.createSftpSource(config)
        
        val oldJob = logJobs[windowId]
        
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            val sftpUri = SftpUri(user, host, port, path, isDirectory = false)
            val sourceId = sftpUri.toString()
            
            state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = sourceId,
                        sourceIds = listOf(sourceId),
                        logs = emptyList(),
                        parserName = parserName,
                        columns = listOf("Timestamp", "Level", "Content"),
                        isDirectory = false
                    )
                }
            }
            onSavePreferences()

            // Update tab title if it's the only window or first window
            val fileName = path.substringAfterLast('/')
            state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = fileName)
                    } else tab
                })
            }
            
            val parser = SimpleLogParser()
            
            sftpSource.observeLogs(LogFilePath(path), parser)
                .collect { result ->
                    result.fold(
                        ifLeft = { error ->
                            state.update { it.updateWindow(windowId) { w -> 
                                w.copy(
                                    error = error.message, 
                                    isLoading = false,
                                    missingSourceIds = w.missingSourceIds + sourceId
                                ) 
                            } }
                        },
                        ifRight = { update ->
                            onHandleLogUpdate(windowId, update, sourceId)
                        }
                    )
                }
        }
    }

    fun connectMultipleSftp(windowId: String, config: SftpConfig, paths: List<String>) {
        val oldJob = logJobs[windowId]
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()

            state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = paths.joinToString(", "),
                        sourceIds = paths.map { "sftp://${config.username.value}@${config.host.value}:${config.port.value}$it" },
                        logs = emptyList(),
                        columns = listOf("Timestamp", "Level", "Content"),
                        isDirectory = false
                    )
                }
            }
            onSavePreferences()

            // Update tab title
            val title = if (paths.size == 1) paths[0].substringAfterLast('/') else "${paths.size} remote files"
            state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = title)
                    } else tab
                })
            }
            
            val parser = SimpleLogParser()
            
            // Create a flow for each path and merge them
            val flows = paths.map { path ->
                val source = logSourceFactory.createSftpSource(config)
                val sId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}$path"
                source.observeLogs(LogFilePath(path), parser).map { result ->
                    result.fold(
                        { l -> (l to sId).left() },
                        { r -> (r to sId).right() }
                    )
                }
            }
            
            flows.merge().collect { result ->
                result.fold(
                    ifLeft = { pair ->
                        val (failure, sId) = pair
                        state.update { it.updateWindow(windowId) { w -> 
                            val sid = failure.sourceId ?: sId
                            val isCritical = sid == w.filePath
                            val newError = if (isCritical) failure.message else w.error
                            w.copy(
                                error = newError, 
                                isLoading = false,
                                missingSourceIds = w.missingSourceIds + sid
                            ) 
                        } }
                    },
                    ifRight = { pair ->
                        val (update, sId) = pair
                        onHandleLogUpdate(windowId, update, sId)
                    }
                )
            }
        }
    }

    fun connectSftpDirectory(windowId: String, config: SftpConfig, path: String, parserName: String? = null) {
        val sftpSource = logSourceFactory.createSftpDirectorySource(config, remoteFileSystem)
        
        val oldJob = logJobs[windowId]
        
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            val sftpUri = SftpUri(config.username.value, config.host.value, config.port.value, path, isDirectory = true)
            val sourceId = sftpUri.toString()
            
            state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = sourceId,
                        sourceIds = listOf(sourceId),
                        logs = emptyList(),
                        parserName = parserName ?: "Auto",
                        isDirectory = true
                    )
                }
            }
            onSavePreferences()

            // Update tab title
            val fileName = path.substringAfterLast('/')
            state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = fileName)
                    } else tab
                })
            }
            
            sftpSource.observeLogs(LogFilePath(path))
                .collect { result ->
                    result.fold(
                        ifLeft = { error ->
                            state.update { it.updateWindow(windowId) { w -> 
                                w.copy(
                                    error = error.message, 
                                    isLoading = false,
                                    missingSourceIds = w.missingSourceIds + sourceId
                                ) 
                            } }
                        },
                        ifRight = { update ->
                            onHandleLogUpdate(windowId, update, sourceId)
                        }
                    )
                }
        }
    }

    private fun handleSingleSftpPath(windowId: String, paths: List<String>, overrideParserName: String?): Boolean {
        if (paths.size != 1 || !paths[0].startsWith("sftp://")) return false
        
        val uri = paths[0]
        val config = workspaceLogLoader.findSftpConfig(uri)
        val sftpUri = SftpUri.parse(uri)
        
        if (config != null && sftpUri != null) {
            if (sftpUri.isDirectory) {
                connectSftpDirectory(windowId, config, sftpUri.path, overrideParserName)
            } else {
                connectSftp(windowId, config.name, config.host.value, config.port.value, config.username.value, config.auth, sftpUri.path, overrideParserName)
            }
            return true
        } else if (sftpUri != null) {
            state.update { it.updateWindow(windowId) { logWindow ->
                logWindow.copy(
                    filePath = uri,
                    sourceIds = listOf(uri),
                    missingSourceIds = setOf(uri),
                    error = "SFTP connection not found in preferences"
                )
            } }
            return true
        }
        return false
    }

    private fun updateWindowStateForLoading(windowId: String, paths: List<String>) {
        val isDir = paths.size == 1 && localFileSystem.isDirectory(paths[0])
        val fileName = if (paths.size == 1) localFileSystem.getName(paths[0]) else "${paths.size} files"
        
        state.update { currentState ->
            currentState.copy(tabs = currentState.tabs.map { tab ->
                val newWindows = tab.windows.map { window ->
                    if (window.id == windowId) {
                        window.copy(
                            isLoading = true, 
                            error = null, 
                            filePath = paths.joinToString(", "), 
                            logs = emptyList(), 
                            sourceIds = paths,
                            isDirectory = isDir
                        )
                    } else window
                }
                val newTitle = if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) fileName else tab.title
                tab.copy(windows = newWindows, title = newTitle)
            })
        }
    }

    private fun updateWindowStateWithParserResults(windowId: String, results: List<ProbeResult?>, overrideParserName: String?) {
        state.update { currentState ->
            currentState.updateWindow(windowId) { window ->
                window.copy(
                    columns = results.firstNotNullOfOrNull { it?.columns } ?: listOf("Timestamp", "Level", "Content"),
                    parserName = if (results.size > 1 && overrideParserName == null) "Multiple" else (overrideParserName ?: results.firstOrNull()?.parserName ?: "Auto")
                )
            }
        }
    }

    private suspend fun handleLogLoadingFailure(windowId: String, path: String, failure: LogFailure) {
        val message = failure.message
        logger.error { "Failed to load logs from $path: $message" }
        state.update { currentState ->
            currentState.updateWindow(windowId) { window ->
                val sourceId = failure.sourceId ?: path
                val newMissing = window.missingSourceIds + sourceId
                val isCritical = sourceId == window.filePath || path == window.filePath
                val newError = if (isCritical) message else window.error
                window.copy(isLoading = false, error = newError, missingSourceIds = newMissing)
            }
        }
        val currentWindow = state.value.tabs.flatMap { it.windows }.find { it.id == windowId }
        if (currentWindow?.error != null || currentWindow?.logs?.isEmpty() == true) {
            onShowError(message)
        }
    }
}
