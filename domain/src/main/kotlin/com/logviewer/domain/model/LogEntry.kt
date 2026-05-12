package com.logviewer.domain.model

data class LogEntry(
    val timestamp: LogTimestamp,
    val level: LogLevel,
    val content: LogContent
)
