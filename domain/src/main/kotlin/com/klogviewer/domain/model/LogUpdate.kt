package com.klogviewer.domain.model

sealed interface LogUpdate {
    data class Initial(val entries: List<LogEntry>) : LogUpdate
    data class Appended(val entries: List<LogEntry>) : LogUpdate
    data class SourceMissing(val sourceId: String) : LogUpdate
    object Reset : LogUpdate
}
