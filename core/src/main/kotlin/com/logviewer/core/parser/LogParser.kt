package com.logviewer.core.parser

import arrow.core.Either
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogFailure

interface LogParser {
    fun parse(line: String): Either<LogFailure.ParsingError, LogEntry>
}
