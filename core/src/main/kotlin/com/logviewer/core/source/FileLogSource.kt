package com.logviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.core.parser.LogParser
import com.logviewer.domain.model.*
import com.logviewer.domain.repository.LogSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class FileLogSource(
    private val parser: LogParser,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogSource {

    override fun observeLogs(path: LogFilePath): Flow<Either<LogFailure, LogUpdate>> = flow {
        try {
            val file = File(path.value)
            if (!file.exists()) {
                emit(LogFailure.FileError("File does not exist: ${path.value}").left())
                return@flow
            }

            // Using useLines for memory-efficient reading (still reads all for Initial load)
            val entries = file.useLines { lines ->
                lines.mapNotNull { line ->
                    parser.parse(line).getOrNull()
                }.toList()
            }
            
            emit(LogUpdate.Initial(entries).right())
            
            // Future: Tail -f implementation would go here, emitting LogUpdate.Appended
        } catch (e: Exception) {
            emit(LogFailure.FileError("Failed to read log file: ${e.message}", e).left())
        }
    }.flowOn(dispatcher)
}
