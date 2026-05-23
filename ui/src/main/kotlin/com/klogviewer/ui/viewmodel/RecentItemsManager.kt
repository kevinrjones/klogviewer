package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.SftpUri
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.ui.mvi.KLogViewerState

class RecentItemsManager(private val localFileSystem: LocalFileSystem) {
    fun updateRecentItems(state: KLogViewerState, paths: List<String>): KLogViewerState {
        val files = paths.filter { isFile(it) }
        val dirs = paths.filter { isDirectory(it) }
        
        if (files.isEmpty() && dirs.isEmpty()) return state

        val newRecentFiles = (files + state.recentFiles).distinct().take(50)
        val newRecentDirectories = (dirs + state.recentDirectories).distinct().take(50)
        
        return state.copy(
            recentFiles = newRecentFiles,
            recentDirectories = newRecentDirectories
        )
    }

    private fun isFile(path: String): Boolean {
        if (path.startsWith("sftp://")) {
            return SftpUri.parse(path)?.isDirectory == false
        }
        return localFileSystem.isFile(path)
    }

    private fun isDirectory(path: String): Boolean {
        if (path.startsWith("sftp://")) {
            return SftpUri.parse(path)?.isDirectory == true
        }
        return localFileSystem.isDirectory(path)
    }

    fun removeRecentItem(state: KLogViewerState, path: String): KLogViewerState {
        return state.copy(
            recentFiles = state.recentFiles - path,
            recentDirectories = state.recentDirectories - path
        )
    }

    fun clearMissingRecentItems(state: KLogViewerState): KLogViewerState {
        return state.copy(
            recentFiles = state.recentFiles.filter { exists(it) },
            recentDirectories = state.recentDirectories.filter { exists(it) }
        )
    }

    private fun exists(path: String): Boolean {
        if (path.startsWith("sftp://")) return true
        return localFileSystem.exists(path)
    }
}
