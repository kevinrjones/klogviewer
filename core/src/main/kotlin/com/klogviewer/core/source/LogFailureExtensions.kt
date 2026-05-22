package com.klogviewer.core.source

import com.klogviewer.domain.model.LogFailure

fun Throwable.toLogFailure(message: String, sourceId: String? = null): LogFailure {
    return LogFailure.FileError(message, sourceId = sourceId, cause = this)
}

fun String.toFileError(sourceId: String? = null): LogFailure {
    return LogFailure.FileError(this, sourceId = sourceId)
}
