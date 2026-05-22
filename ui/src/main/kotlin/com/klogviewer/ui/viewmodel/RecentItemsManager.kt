package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.ui.mvi.KLogViewerState

class RecentItemsManager(private val localFileSystem: LocalFileSystem) {
    fun updateRecentItems(state: KLogViewerState, paths: List<String>): KLogViewerState {
        val files = paths.filter { localFileSystem.isFile(it) }
        val dirs = paths.filter { localFileSystem.isDirectory(it) }
        
        if (files.isEmpty() && dirs.isEmpty()) return state

        val newRecentFiles = (files + state.recentFiles).distinct().take(50)
        val newRecentDirectories = (dirs + state.recentDirectories).distinct().take(50)
        
        return state.copy(
            recentFiles = newRecentFiles,
            recentDirectories = newRecentDirectories
        )
    }

    fun removeRecentItem(state: KLogViewerState, path: String): KLogViewerState {
        return state.copy(
            recentFiles = state.recentFiles - path,
            recentDirectories = state.recentDirectories - path
        )
    }

    fun clearMissingRecentItems(state: KLogViewerState): KLogViewerState {
        return state.copy(
            recentFiles = state.recentFiles.filter { localFileSystem.exists(it) },
            recentDirectories = state.recentDirectories.filter { localFileSystem.exists(it) }
        )
    }
}
