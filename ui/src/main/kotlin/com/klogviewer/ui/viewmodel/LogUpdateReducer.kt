package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.ui.mvi.LogWindow
import java.time.Instant

object LogUpdateReducer {
    fun reduce(window: LogWindow, update: LogUpdate, sourceId: String?): LogWindow {
        val logsAfterUpdate = calculateLogsAfterUpdate(window, update, sourceId)
        val newMissingSourceIds = calculateMissingSourceIds(window, update, sourceId)
        val newSourceIds = calculateSourceIdsAfterUpdate(window, update, sourceId, logsAfterUpdate)
        val newHiddenSourceIds = window.hiddenSourceIds.intersect(newSourceIds.toSet())

        return window.copy(
            isLoading = false,
            logs = logsAfterUpdate,
            sourceIds = newSourceIds,
            hiddenSourceIds = newHiddenSourceIds,
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
            is LogUpdate.Appended -> {
                if (window.sourceIds.size > 1) {
                    insertByTimestamp(window.logs, update.entries)
                } else {
                    window.logs + update.entries
                }
            }
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

        // Ensure logs are sorted by timestamp if we have multiple sources.
        // Appended entries are already inserted at their timestamp position.
        return if (window.sourceIds.size > 1 && update !is LogUpdate.Appended) {
            mergedLogs.sortedBy { it.timestamp.value }
        } else {
            mergedLogs
        }
    }

    /**
     * Inserts live-tail entries at their timestamp position using a near-tail
     * binary search. Entries without a parseable timestamp anchor to file order
     * (appended after the last timestamped entry).
     */
    internal fun insertByTimestamp(existing: List<LogEntry>, newEntries: List<LogEntry>): List<LogEntry> {
        if (newEntries.isEmpty()) return existing
        val result = ArrayList<LogEntry>(existing.size + newEntries.size)
        result.addAll(existing)
        for (entry in newEntries) {
            val instant = entry.instant
            if (instant == null) {
                result.add(entry)
            } else {
                result.add(findInsertionIndex(result, instant), entry)
            }
        }
        return result
    }

    private fun findInsertionIndex(entries: List<LogEntry>, instant: Instant): Int {
        val lastInstant = entries.lastOrNull()?.instant
        if (lastInstant == null || !instant.isBefore(lastInstant)) return entries.size

        var low = 0
        var high = entries.size
        while (low < high) {
            val mid = (low + high) / 2
            val midInstant = entries[mid].instant
            if (midInstant != null && midInstant.isAfter(instant)) {
                high = mid
            } else {
                low = mid + 1
            }
        }
        return low
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
