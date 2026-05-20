package com.klogviewer.domain.model

sealed interface LogFailure {
    val message: String
    val sourceId: String? get() = null
    data class FileError(override val message: String, override val sourceId: String? = null, val cause: Throwable? = null) : LogFailure
    data class ParsingError(override val message: String, val line: String, override val sourceId: String? = null) : LogFailure
}
