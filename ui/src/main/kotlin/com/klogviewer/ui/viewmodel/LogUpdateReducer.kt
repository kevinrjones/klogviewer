package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.ui.mvi.LogWindow

object LogUpdateReducer {
    fun reduce(window: LogWindow, update: LogUpdate, sourceId: String?): LogWindow {
        val logsAfterUpdate = calculateLogsAfterUpdate(window, update, sourceId)
        val newMissingSourceIds = calculateMissingSourceIds(window, update, sourceId)
        val newSourceIds = calculateSourceIdsAfterUpdate(window, update, sourceId, logsAfterUpdate)

        return window.copy(
            isLoading = false,
            logs = logsAfterUpdate,
            sourceIds = newSourceIds,
            missingSourceIds = newMissingSourceIds,
            error = if (newMissingSourceIds.contains(window.filePath)) window.error else null
        )
    }

    private fun calculateLogsAfterUpdate(window: LogWindow, update: LogUpdate, sourceId: String?): List<LogEntry> {
        val mergedLogs = when (update) {
            is LogUpdate.Initial -> {
                if (sourceId != null) {
                    // Additive for specific source, replace existing entries for that source
                    window.logs.filter { it.sourceId != sourceId } + update.entries
                } else {
                    update.entries
                }
            }
            is LogUpdate.Appended -> window.logs + update.entries
            LogUpdate.Reset -> emptyList()
            is LogUpdate.SourceMissing -> {
                val isDirectorySubSource = sourceId != null && sourceId != update.sourceId
                if (isDirectorySubSource) {
                    window.logs.filter { it.sourceId != update.sourceId }
                } else {
                    window.logs
                }
            }
        }

        // Ensure logs are sorted by timestamp if we have multiple sources
        return if (window.sourceIds.size > 1) {
            mergedLogs.sortedBy { it.timestamp.value }
        } else {
            mergedLogs
        }
    }

    private fun calculateMissingSourceIds(window: LogWindow, update: LogUpdate, sourceId: String?): Set<String> {
        val currentMissing = if (sourceId != null) window.missingSourceIds - sourceId else window.missingSourceIds
        return when (update) {
            is LogUpdate.SourceMissing -> currentMissing + update.sourceId
            is LogUpdate.Initial -> currentMissing - update.entries.mapNotNull { it.sourceId }.toSet()
            is LogUpdate.Appended -> currentMissing - update.entries.mapNotNull { it.sourceId }.toSet()
            LogUpdate.Reset -> emptySet()
        }
    }

    private fun calculateSourceIdsAfterUpdate(window: LogWindow, update: LogUpdate, sourceId: String?, logs: List<LogEntry>): List<String> {
        // Extract unique source IDs from the logs to ensure badges are shown for all discovered files
        val discoveredSourceIds = logs.mapNotNull { it.sourceId }.distinct().filter { it.isNotEmpty() }

        val currentSourceIds = if (update is LogUpdate.SourceMissing) {
            val isDirectorySubSource = sourceId != null && sourceId != update.sourceId
            if (isDirectorySubSource) {
                window.sourceIds - update.sourceId
            } else {
                window.sourceIds
            }
        } else {
            window.sourceIds
        }

        return (currentSourceIds + discoveredSourceIds).distinct()
    }
}
