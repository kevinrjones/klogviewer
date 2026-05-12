package com.logviewer.core.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.logviewer.core.parser.LogParser
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogFailure
import com.logviewer.domain.model.LogFilePath
import java.io.File

class LogService(private val parser: LogParser) {
    fun loadLogs(path: LogFilePath): Either<LogFailure.FileError, List<LogEntry>> {
        return try {
            val file = File(path.value)
            if (!file.exists()) {
                return LogFailure.FileError("File does not exist: ${path.value}").left()
            }
            file.readLines()
                .mapNotNull { line ->
                    parser.parse(line).getOrNull()
                }.right()
        } catch (e: Exception) {
            LogFailure.FileError("Failed to read log file: ${e.message}", e).left()
        }
    }
}
