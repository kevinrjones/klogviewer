package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel

class LevelMapper(
    private val customMappings: Map<String, LogLevel> = emptyMap(),
    private val usePrefixMatching: Boolean = false,
    private val defaultLevel: LogLevel = LogLevel.UNKNOWN
) {
    private val defaultMappings = mapOf(
        "TRACE" to LogLevel.DEBUG,
        "DEBUG" to LogLevel.DEBUG,
        "DBUG" to LogLevel.DEBUG,
        "VERBOSE" to LogLevel.DEBUG,
        "INFO" to LogLevel.INFO,
        "INF" to LogLevel.INFO,
        "INFORMATION" to LogLevel.INFO,
        "NOTICE" to LogLevel.INFO,
        "WARN" to LogLevel.WARN,
        "WRN" to LogLevel.WARN,
        "WARNING" to LogLevel.WARN,
        "ERROR" to LogLevel.ERROR,
        "ERR" to LogLevel.ERROR,
        "SEVERE" to LogLevel.ERROR,
        "FATAL" to LogLevel.FATAL,
        "FTL" to LogLevel.FATAL,
        "CRITICAL" to LogLevel.FATAL,
        // Numeric levels (Syslog RFC 5424)
        "0" to LogLevel.FATAL, // Emergency
        "1" to LogLevel.FATAL, // Alert
        "2" to LogLevel.FATAL, // Critical
        "3" to LogLevel.ERROR, // Error
        "4" to LogLevel.WARN,  // Warning
        "5" to LogLevel.INFO,  // Notice
        "6" to LogLevel.INFO,  // Informational
        "7" to LogLevel.DEBUG  // Debug
    )

    private val allMappings = defaultMappings + customMappings

    fun map(input: String?): LogLevel {
        if (input == null) return defaultLevel
        
        val normalized = input.trim()
            .removePrefix("[").removeSuffix("]")
            .removePrefix("(").removeSuffix(")")
            .uppercase()
        if (normalized.isEmpty()) return defaultLevel

        // 1. Exact or Alias Match
        allMappings[normalized]?.let { return it }

        // 2. Prefix Matching (if enabled)
        if (usePrefixMatching) {
            when (normalized.first()) {
                'D', 'T' -> return LogLevel.DEBUG
                'I' -> return LogLevel.INFO
                'W' -> return LogLevel.WARN
                'E', 'S' -> return LogLevel.ERROR
                'F', 'C' -> return LogLevel.FATAL
            }
        }

        return defaultLevel
    }
}
