package com.klogviewer.ui.viewmodel

import java.math.BigDecimal

internal sealed interface LogQueryExpression {
    data class TextQuery(
        val text: String
    ) : LogQueryExpression

    data class FieldPredicate(
        val path: String,
        val operator: FieldOperator,
        val value: QueryLiteral? = null,
        val explicitFieldPrefix: Boolean = false
    ) : LogQueryExpression

    data class LegacyDashboardFieldQuery(
        val field: String,
        val value: String
    ) : LogQueryExpression

    data class BooleanExpression(
        val operator: BooleanOperator,
        val left: LogQueryExpression,
        val right: LogQueryExpression
    ) : LogQueryExpression
}

internal enum class BooleanOperator {
    AND,
    OR
}

internal enum class FieldOperator {
    EQUALS,
    CONTAINS,
    REGEX,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    EXISTS,
    MISSING,
    IS_NULL
}

internal sealed interface QueryLiteral {
    data class StringValue(val value: String) : QueryLiteral
    data class NumberValue(val value: BigDecimal) : QueryLiteral
    data class BooleanValue(val value: Boolean) : QueryLiteral
    data object NullValue : QueryLiteral
}

/**
 * Grammar overview for structured filters:
 * - `level:error`, `has:trace.id`
 * - `field:path=value`
 * - `field:path op value` where `op` is one of `=`, `contains`, `~`, `>`, `>=`, `<`, `<=`, `exists`, `missing`
 * - canonical `path op value` forms for known aliases (`level`, `message`, `trace.id`) and dotted/indexed paths
 * - boolean composition with `AND` / `OR` and parentheses
 *
 * Any malformed or unsupported structured syntax falls back to `TextQuery` so existing free-text behavior remains non-blocking.
 */
internal object LogQueryParser {
    private val expressionParser = LogQueryExpressionParser()

    fun parse(query: String): LogQueryExpression {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return LogQueryExpression.TextQuery(query)
        }

        val parsedExpression = expressionParser.parse(trimmed)
        return parsedExpression ?: LogQueryExpression.TextQuery(query)
    }
}