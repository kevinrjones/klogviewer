package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SftpIntentHandler(
    private val remoteFileSystem: RemoteFileSystem,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<KLogViewerState>,
    private val recentItemsManager: RecentItemsManager,
    private val onSavePreferences: () -> Unit,
    private val onLoadFiles: (windowId: String, paths: List<String>) -> Unit,
    private val onConnectSftp: (windowId: String, name: String, host: String, port: Int, user: String, auth: SftpAuth, path: String) -> Unit,
    private val onConnectMultipleSftp: (windowId: String, config: SftpConfig, paths: List<String>) -> Unit,
    private val onConnectSftpDirectory: (windowId: String, config: SftpConfig, path: String) -> Unit,
    private val onHandleBrowse: (config: SftpConfig, path: String) -> Unit
) {
    fun handle(intent: KLogViewerIntent.SftpIntent) {
        when (intent) {
            is KLogViewerIntent.ConnectSftp -> handleConnectSftp(intent)
            is KLogViewerIntent.ConnectMultipleSftp -> handleConnectMultipleSftp(intent)
            is KLogViewerIntent.ConnectSftpDirectory -> handleConnectSftpDirectory(intent)
            is KLogViewerIntent.BrowseSftp -> onHandleBrowse(intent.config, intent.path)
            is KLogViewerIntent.NavigateRemote -> handleNavigateRemote(intent)
            is KLogViewerIntent.SaveSftpConnection -> handleSaveSftpConnection(intent)
            is KLogViewerIntent.DeleteSftpConnection -> handleDeleteSftpConnection(intent)
        }
    }

    private fun handleConnectSftp(intent: KLogViewerIntent.ConnectSftp) {
        val activeWindowId = state.value.activeTab?.activeWindow?.id
        if (activeWindowId != null) {
            val config = SftpConfig(
                intent.name, 
                Host(intent.host), 
                Port(intent.port), 
                Username(intent.user), 
                intent.auth, 
                intent.path
            )

            saveSftpConnection(config)

            if (intent.addToWorkspace) {
                val sftpUri = SftpUri(config.username.value, config.host.value, config.port.value, intent.path).toString()
                val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == activeWindowId }?.sourceIds ?: emptyList()
                val newPaths = (currentPaths + sftpUri).distinct()
                onLoadFiles(activeWindowId, newPaths)
                state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), listOf(sftpUri)) }
            } else {
                scope.launch {
                    state.update { it.copy(isRemoteLoading = true) }
                    val result = remoteFileSystem.listFiles(config, intent.path)
                    state.update { it.copy(isRemoteLoading = false) }

                    result.fold(
                        { _ ->
                            val sftpUri = SftpUri(config.username.value, config.host.value, config.port.value, intent.path).toString()
                            onConnectSftp(activeWindowId, intent.name, intent.host, intent.port, intent.user, intent.auth, intent.path)
                            state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), listOf(sftpUri)) }
                        },
                        { files ->
                            val exactMatch = files.find { it.path == intent.path || it.path == intent.path.removeSuffix("/") + "/" + it.name }

                            if (files.size == 1 && exactMatch != null && !exactMatch.isDirectory) {
                                val sftpUri = SftpUri(config.username.value, config.host.value, config.port.value, intent.path).toString()
                                onConnectSftp(activeWindowId, intent.name, intent.host, intent.port, intent.user, intent.auth, intent.path)
                                state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), listOf(sftpUri)) }
                            } else {
                                state.update {
                                    it.copy(
                                        remoteFiles = files.filter { f -> f.name != "." && f.name != ".." },
                                        remoteBrowsePath = intent.path,
                                        currentSftpConfig = config,
                                        pendingDialog = KLogViewerState.DialogType.SFTP_BROWSE,
                                        isAddMode = intent.addToWorkspace
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun handleConnectMultipleSftp(intent: KLogViewerIntent.ConnectMultipleSftp) {
        saveSftpConnection(intent.config)
        val activeWindowId = state.value.activeTab?.activeWindow?.id
        if (activeWindowId != null) {
            val newUris = intent.paths.map { "sftp://${intent.config.username.value}@${intent.config.host.value}:${intent.config.port.value}$it" }
            if (intent.addToWorkspace) {
                val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == activeWindowId }?.sourceIds ?: emptyList()
                val newPaths = (currentPaths + newUris).distinct()
                onLoadFiles(activeWindowId, newPaths)
            } else {
                onConnectMultipleSftp(activeWindowId, intent.config, intent.paths)
            }
            state.update { recentItemsManager.updateRecentItems(it, newUris) }
        }
        state.update { it.copy(pendingDialog = null) }
    }

    private fun handleConnectSftpDirectory(intent: KLogViewerIntent.ConnectSftpDirectory) {
        saveSftpConnection(intent.config)
        val activeWindowId = state.value.activeTab?.activeWindow?.id
        if (activeWindowId != null) {
            val sftpUri = SftpUri(intent.config.username.value, intent.config.host.value, intent.config.port.value, intent.path, isDirectory = true).toString()
            if (intent.addToWorkspace) {
                val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == activeWindowId }?.sourceIds ?: emptyList()
                val newPaths = (currentPaths + sftpUri).distinct()
                onLoadFiles(activeWindowId, newPaths)
            } else {
                onConnectSftpDirectory(activeWindowId, intent.config, intent.path)
            }
            state.update { recentItemsManager.updateRecentItems(it, listOf(sftpUri)) }
        }
        state.update { it.copy(pendingDialog = null) }
    }

    private fun handleNavigateRemote(intent: KLogViewerIntent.NavigateRemote) {
        val config = state.value.currentSftpConfig
        if (config != null) {
            onHandleBrowse(config, intent.path)
        }
    }

    private fun handleSaveSftpConnection(intent: KLogViewerIntent.SaveSftpConnection) {
        saveSftpConnection(intent.config)
    }

    private fun handleDeleteSftpConnection(intent: KLogViewerIntent.DeleteSftpConnection) {
        state.update { currentState ->
            currentState.copy(sftpConnections = currentState.sftpConnections.filter { it.name != intent.name })
        }
        onSavePreferences()
    }

    private fun saveSftpConnection(config: SftpConfig) {
        state.update { currentState ->
            val updatedList = (currentState.sftpConnections.filter { it.name != config.name } + config)
                .sortedBy { it.name }
            currentState.copy(sftpConnections = updatedList)
        }
        onSavePreferences()
    }
}
