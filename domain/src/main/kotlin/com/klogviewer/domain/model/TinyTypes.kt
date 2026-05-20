package com.klogviewer.domain.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class LogFilePath(val value: String) {
    init {
        require(value.isNotBlank()) { "Log file path cannot be blank" }
    }
}

@Serializable
@JvmInline
value class LogTimestamp(val value: String)

@Serializable
@JvmInline
value class LogContent(val value: String)

@Serializable
@JvmInline
value class Host(val value: String)

@Serializable
@JvmInline
value class Port(val value: Int)

@Serializable
@JvmInline
value class Username(val value: String)
