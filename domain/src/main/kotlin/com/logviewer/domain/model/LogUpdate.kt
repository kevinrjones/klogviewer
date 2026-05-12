package com.logviewer.domain.model

sealed interface LogUpdate {
    data class Initial(val entries: List<LogEntry>) : LogUpdate
    data class Appended(val entries: List<LogEntry>) : LogUpdate
    object Reset : LogUpdate
}
