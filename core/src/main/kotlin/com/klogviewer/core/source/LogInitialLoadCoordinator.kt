package com.klogviewer.core.source

import com.klogviewer.domain.model.LogEntry

class LogInitialLoadCoordinator {
    private val currentEntries = mutableMapOf<String, MutableList<LogEntry>>()
    private val filesAttemptedInitial = mutableSetOf<String>()
    
    fun onInitialLoad(file: String, entries: List<LogEntry>) {
        synchronized(this) {
            currentEntries.getOrPut(file) { mutableListOf() }.addAll(entries)
            filesAttemptedInitial.add(file)
        }
    }
    
    fun onAppendedDuringInitial(file: String, entries: List<LogEntry>) {
        synchronized(this) {
            currentEntries.getOrPut(file) { mutableListOf() }.addAll(entries)
        }
    }
    
    fun onFileFailedInitial(file: String) {
        synchronized(this) {
            filesAttemptedInitial.add(file)
        }
    }
    
    fun isComplete(totalFiles: Int): Boolean {
        synchronized(this) {
            return totalFiles == 0 || filesAttemptedInitial.size >= totalFiles
        }
    }
    
    fun getAggregatedInitialEntries(): List<LogEntry> {
        synchronized(this) {
            return currentEntries.values.flatten().sortedBy { it.timestamp.value }
        }
    }
}
