package com.klogviewer.domain.parser

import arrow.core.Either
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogFailure

interface LogParser {
    fun parse(line: String): Either<LogFailure.ParsingError, LogEntry>
}
