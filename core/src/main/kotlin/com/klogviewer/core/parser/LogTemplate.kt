package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel

data class LogTemplate(
    val name: String,
    val regex: String,
    val timestampPattern: String,
    val levelMapper: LevelMapper = LevelMapper(),
    val columns: List<String> = emptyList()
)
