package com.klogviewer.domain.model

import java.time.Instant

data class LogEntry(
    val timestamp: LogTimestamp,
    val level: LogLevel,
    val content: LogContent,
    val fields: Map<String, String> = emptyMap(),
    val sourceId: String? = null,
    val instant: Instant? = null,
    val structuredData: StructuredLogData? = null
) {
    fun compatibilityFields(): Map<String, String> {
        val projectedFields = structuredData?.toCompatibilityFields().orEmpty()
        return projectedFields + fields
    }
}
