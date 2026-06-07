package com.klogviewer.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN
}
