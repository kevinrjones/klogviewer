package com.klogviewer.domain.model

@JvmInline
value class LogFilePath(val value: String) {
    init {
        require(value.isNotBlank()) { "Log file path cannot be blank" }
    }
}

@JvmInline
value class LogTimestamp(val value: String)

@JvmInline
value class LogContent(val value: String)
