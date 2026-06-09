package com.klogviewer.core.parser

object CanonicalFieldAliases {
    const val CANONICAL_TIMESTAMP = "timestamp"
    const val CANONICAL_LEVEL = "level"
    const val CANONICAL_MESSAGE = "message"
    const val CANONICAL_MESSAGE_TEMPLATE = "message.template"
    const val CANONICAL_LOGGER = "logger"
    const val CANONICAL_EXCEPTION = "exception"
    const val CANONICAL_TRACE_ID = "trace.id"
    const val CANONICAL_SPAN_ID = "span.id"
    const val CANONICAL_CORRELATION_ID = "correlation.id"

    val TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "timestamp",
        "@timestamp",
        "time",
        "ts",
        "@t",
        "Timestamp",
        "timeMillis",
        "timeUnixNano"
    )
    val LEVEL_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "level",
        "severity",
        "lvl",
        "@l",
        "LogLevel",
        "Level",
        "severityText"
    )
    val MESSAGE_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "message",
        "msg",
        "body",
        "@m",
        "RenderedMessage",
        "Message",
        "@mt",
        "MessageTemplate",
        "OriginalFormat"
    )
    val MESSAGE_TEMPLATE_ALIASES_IN_PRECEDENCE_ORDER = listOf("@mt", "MessageTemplate", "OriginalFormat")
    val CONTENT_KEYS_IN_PRECEDENCE_ORDER = listOf(
        "message",
        "msg",
        "content",
        "body",
        "@m",
        "RenderedMessage",
        "Message",
        "@mt",
        "log"
    )
    val LOGGER_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "logger",
        "logger_name",
        "loggerName",
        "SourceContext",
        "Category",
        "CategoryName",
        "LoggerName"
    )
    val EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER = listOf("exception", "error", "stackTrace", "Exception", "@x", "thrown")
    val TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER = listOf("trace.id", "traceId", "TraceId", "@tr", "trace_id")
    val SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER = listOf("span.id", "spanId", "SpanId", "@sp", "span_id")
    val CORRELATION_ID_ALIASES_IN_PRECEDENCE_ORDER = listOf(
        "correlation.id",
        "correlationId",
        "CorrelationId",
        "RequestId",
        "requestId"
    )

    val CANONICAL_ALIASES_IN_PRECEDENCE_ORDER = linkedMapOf(
        CANONICAL_TIMESTAMP to TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_LEVEL to LEVEL_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_MESSAGE to MESSAGE_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_MESSAGE_TEMPLATE to MESSAGE_TEMPLATE_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_LOGGER to LOGGER_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_EXCEPTION to EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_TRACE_ID to TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_SPAN_ID to SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_CORRELATION_ID to CORRELATION_ID_ALIASES_IN_PRECEDENCE_ORDER
    )

    val QUERY_CANONICAL_ALIAS_PATHS = mapOf(
        CANONICAL_TRACE_ID to TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_SPAN_ID to SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_CORRELATION_ID to CORRELATION_ID_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_LEVEL to LEVEL_ALIASES_IN_PRECEDENCE_ORDER,
        CANONICAL_MESSAGE to MESSAGE_ALIASES_IN_PRECEDENCE_ORDER
    )

    val QUERY_SHORT_FORM_CANONICAL_KEYS = QUERY_CANONICAL_ALIAS_PATHS.keys

    val confidenceAliasGroups: List<Set<String>> = listOf(
        TIMESTAMP_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        LEVEL_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        CONTENT_KEYS_IN_PRECEDENCE_ORDER.toSet(),
        MESSAGE_TEMPLATE_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        LOGGER_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        EXCEPTION_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        TRACE_ID_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        SPAN_ID_ALIASES_IN_PRECEDENCE_ORDER.toSet(),
        CORRELATION_ID_ALIASES_IN_PRECEDENCE_ORDER.toSet()
    )
}
