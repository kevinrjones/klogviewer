package com.klogviewer.domain.model

sealed interface LogFailure {
    val message: String
    data class FileError(override val message: String, val cause: Throwable? = null) : LogFailure
    data class ParsingError(override val message: String, val line: String) : LogFailure
}
