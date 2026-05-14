package com.logviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.domain.model.LogFailure
import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogUpdate
import com.logviewer.domain.repository.LogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class MergedLogSource(
    private val sources: List<Pair<LogSource, LogFilePath>>
) {
    fun observeMerged(): Flow<Either<LogFailure, LogUpdate>> = flow {
        if (sources.isEmpty()) {
            emit(LogUpdate.Initial(emptyList()).right())
            return@flow
        }

        val initialResults = sources.map { (source, path) ->
            source.observeLogs(path).first()
        }

        val failures = initialResults.mapNotNull { it.fold({ it }, { null }) }
        if (failures.isNotEmpty()) {
            emit(failures.first().left())
            return@flow
        }

        val allEntries = initialResults.mapNotNull { result ->
            result.fold({ null }, { it as? LogUpdate.Initial })
        }
            .flatMap { it.entries }
            .sortedBy { it.timestamp.value }

        emit(LogUpdate.Initial(allEntries).right())

        // Note: For now, we don't support real-time merging of appends in MergedLogSource.
        // This will be added when Tail -f is fully implemented in FileLogSource.
    }
}
