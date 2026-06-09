package com.klogviewer.domain.model

import java.time.Instant
import kotlin.LazyThreadSafetyMode

data class LogEntry(
    val timestamp: LogTimestamp,
    val level: LogLevel,
    val content: LogContent,
    val fields: Map<String, String> = emptyMap(),
    val sourceId: String? = null,
    val instant: Instant? = null,
    val structuredData: StructuredLogData? = null
) {
    private val compatibilityProjection: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        val projectedFields = structuredData?.toCompatibilityFields().orEmpty()
        projectedFields + fields
    }

    fun compatibilityFields(): Map<String, String> {
        return compatibilityProjection
    }

    fun resolvedLevelKey(): String {
        return fields[RAW_LEVEL_FIELD]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
            ?: level.name
    }

    companion object {
        const val RAW_LEVEL_FIELD: String = "level"
    }
}
