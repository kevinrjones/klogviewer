package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class S3IntentHandler(
    private val remoteFileSystem: RemoteFileSystem,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<KLogViewerState>,
    private val recentItemsManager: RecentItemsManager,
    private val onSavePreferences: () -> Unit,
    private val onLoadFiles: (String, List<String>) -> Unit,
    private val onConnectS3: (String, S3Config) -> Unit,
    private val onConnectMultipleS3: (String, S3Config, List<String>) -> Unit,
    private val onConnectS3Directory: (String, S3Config, String) -> Unit,
    private val onHandleBrowse: (S3Config, String) -> Unit
) {
    fun handle(intent: KLogViewerIntent.S3Intent) {
        when (intent) {
            is KLogViewerIntent.ConnectS3 -> handleConnectS3(intent)
            is KLogViewerIntent.ConnectMultipleS3 -> handleConnectMultipleS3(intent)
            is KLogViewerIntent.ConnectS3Directory -> handleConnectS3Directory(intent)
            is KLogViewerIntent.BrowseS3 -> {
                onHandleBrowse(intent.config, intent.prefix)
            }
            is KLogViewerIntent.SaveS3Connection -> {
                state.update { 
                    it.copy(s3Connections = it.s3Connections.filter { c -> c.name != intent.config.name } + intent.config)
                }
                onSavePreferences()
            }
            is KLogViewerIntent.DeleteS3Connection -> {
                state.update {
                    it.copy(s3Connections = it.s3Connections.filter { c -> c.name != intent.name })
                }
                onSavePreferences()
            }
        }
    }

    private fun handleConnectS3(intent: KLogViewerIntent.ConnectS3) {
        val windowId = state.value.activeTab?.activeWindowId ?: "default-window"
        val s3Uri = com.klogviewer.domain.model.S3Uri(intent.config.bucket, intent.config.prefix, isDirectory = false).toString()
        
        if (intent.addToWorkspace) {
            val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == windowId }?.sourceIds ?: emptyList()
            val newPaths = (currentPaths + s3Uri).distinct()
            onLoadFiles(windowId, newPaths)
        } else {
            onConnectS3(windowId, intent.config)
        }
        state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), listOf(s3Uri)) }
    }

    private fun handleConnectMultipleS3(intent: KLogViewerIntent.ConnectMultipleS3) {
        val windowId = state.value.activeTab?.activeWindowId ?: "default-window"
        val newUris = intent.keys.map { "s3://${intent.config.bucket}$it" }
        
        if (intent.addToWorkspace) {
            val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == windowId }?.sourceIds ?: emptyList()
            val newPaths = (currentPaths + newUris).distinct()
            onLoadFiles(windowId, newPaths)
        } else {
            onConnectMultipleS3(windowId, intent.config, intent.keys)
        }
        state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), newUris) }
    }

    private fun handleConnectS3Directory(intent: KLogViewerIntent.ConnectS3Directory) {
        val windowId = state.value.activeTab?.activeWindowId ?: "default-window"
        val s3Uri = com.klogviewer.domain.model.S3Uri(intent.config.bucket, intent.prefix, isDirectory = true).toString()
        
        if (intent.addToWorkspace) {
            val currentPaths = state.value.tabs.flatMap { it.windows }.find { it.id == windowId }?.sourceIds ?: emptyList()
            val newPaths = (currentPaths + s3Uri).distinct()
            onLoadFiles(windowId, newPaths)
        } else {
            onConnectS3Directory(windowId, intent.config, intent.prefix)
        }
        state.update { recentItemsManager.updateRecentItems(it.copy(pendingDialog = null), listOf(s3Uri)) }
    }
}
