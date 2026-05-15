package com.logviewer.core.parser

import com.logviewer.domain.model.LogLevel

data class LogTemplate(
    val name: String,
    val regex: String,
    val timestampPattern: String,
    val levelMapper: LevelMapper = LevelMapper(),
    val columns: List<String> = emptyList()
)
