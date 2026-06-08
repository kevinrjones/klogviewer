package com.klogviewer.core.parser

object CanonicalFieldAliases {
    const val CANONICAL_TIMESTAMP = "timestamp"
    const val CANONICAL_LEVEL = "level"
    const val CANONICAL_MESSAGE = "message"
    const val CANONICAL_LOGGER = "logger"
    const val CANONICAL_EXCEPTION = "exception"
    const val CANONICAL_TRACE_ID = "trace.id"
    const val CANONICAL_SPAN_ID = "span.id"

    val TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER = listOf("timestamp", "@timestamp", "time", "ts", "@t", "Timestamp")
    val LEVEL_ALIASES_IN_PRECEDENCE_ORDER = listOf("level", "severity", "lvl", "@l", "LogLevel", "Level")
    val MESSAGE_ALIASES_IN_PRECEDENCE_ORDER = listOf("message", "msg", "body", "@m", "Message", "@mt")
    val CONTENT_KEYS_IN_PRECEDENCE_ORDER = listOf("message", "msg", "content", "body", "@m", "@mt", "Message")
    val LOGGER_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "logger",
        "logger_name",
        "SourceContext",
        "Category",
        "CategoryName"
    )
    val EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER = listOf("exception", "error", "stackTrace", "Exception", "@x")
    val TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER = listOf("traceId", "TraceId", "@tr")
    val SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER = listOf("spanId", "SpanId", "@sp")

    val confidenceAliasGroups: List<Set<String>> = listOf(
        TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        LEVEL_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        CONTENT_KEYS_IN_PRECEDENCE_ORDER.toSet(),
        LOGGER_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER.toSet()
    )
}
