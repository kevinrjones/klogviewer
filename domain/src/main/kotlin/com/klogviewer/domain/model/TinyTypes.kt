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

@JvmInline
value class Host(val value: String)

@JvmInline
value class Port(val value: Int)

@JvmInline
value class Username(val value: String)
