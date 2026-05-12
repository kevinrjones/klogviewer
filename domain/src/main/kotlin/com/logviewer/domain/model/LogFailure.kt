package com.logviewer.domain.model

sealed interface LogFailure {
    data class FileError(val message: String, val cause: Throwable? = null) : LogFailure
    data class ParsingError(val message: String, val line: String) : LogFailure
}
