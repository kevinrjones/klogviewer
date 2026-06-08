package com.klogviewer.ui.viewmodel

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.math.BigDecimal

class LogQueryParserTest {

    @Test
    fun `parses explicit compact field equals predicate`() {
        val expression = LogQueryParser.parse("field:Properties.UserId=\"u-123\"")

        expectThat(expression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "Properties.UserId",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("u-123"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses explicit equals alias operator`() {
        val expression = LogQueryParser.parse("field:Thread_name eq \"eventLoopGroupProxy-4-4\"")

        expectThat(expression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "Thread_name",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("eventLoopGroupProxy-4-4"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses explicit numeric comparison predicate`() {
        val expression = LogQueryParser.parse("field:StatusCode >= 500")

        expectThat(expression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "StatusCode",
                operator = FieldOperator.GREATER_THAN_OR_EQUAL,
                value = QueryLiteral.NumberValue(BigDecimal("500")),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses exists and missing predicates`() {
        val existsExpression = LogQueryParser.parse("field:TraceId exists")
        val missingExpression = LogQueryParser.parse("field:TraceId missing")

        expectThat(existsExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "TraceId",
                operator = FieldOperator.EXISTS,
                explicitFieldPrefix = true
            )
        )
        expectThat(missingExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "TraceId",
                operator = FieldOperator.MISSING,
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses canonical short forms`() {
        val hasExpression = LogQueryParser.parse("has:trace.id")
        val levelExpression = LogQueryParser.parse("level:error")

        expectThat(hasExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "trace.id",
                operator = FieldOperator.EXISTS,
                explicitFieldPrefix = false
            )
        )
        expectThat(levelExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "level",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("error"),
                explicitFieldPrefix = false
            )
        )
    }

    @Test
    fun `parses canonical contains predicate`() {
        val expression = LogQueryParser.parse("message contains \"timeout\"")

        expectThat(expression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "message",
                operator = FieldOperator.CONTAINS,
                value = QueryLiteral.StringValue("timeout"),
                explicitFieldPrefix = false
            )
        )
    }

    @Test
    fun `parses regex predicate`() {
        val expression = LogQueryParser.parse("field:message ~ \"timeout|deadline\"")

        expectThat(expression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "message",
                operator = FieldOperator.REGEX,
                value = QueryLiteral.StringValue("timeout|deadline"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses escaped quoted values`() {
        val escapedQuoteExpression = LogQueryParser.parse("field:message contains \"failed \\\"hard\\\"\"")
        val escapedSlashExpression = LogQueryParser.parse("field:path = \"C:\\\\logs\\\\app.log\"")

        expectThat(escapedQuoteExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "message",
                operator = FieldOperator.CONTAINS,
                value = QueryLiteral.StringValue("failed \"hard\""),
                explicitFieldPrefix = true
            )
        )
        expectThat(escapedSlashExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "path",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("C:\\logs\\app.log"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses escaped path segments and indexed paths`() {
        val rootLiteralSegment = LogQueryParser.parse("field:`Properties.User.Id`=\"u-123\"")
        val nestedLiteralSegment = LogQueryParser.parse("field:Properties.`User.Id`=\"u-123\"")
        val indexedLiteralSegment = LogQueryParser.parse("field:items[0].`id.with.dot`=\"a1\"")

        expectThat(rootLiteralSegment).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "Properties\\.User\\.Id",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("u-123"),
                explicitFieldPrefix = true
            )
        )
        expectThat(nestedLiteralSegment).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "Properties.User\\.Id",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("u-123"),
                explicitFieldPrefix = true
            )
        )
        expectThat(indexedLiteralSegment).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "items[0].id\\.with\\.dot",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("a1"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses typed literals and quoted literal strings`() {
        val numberExpression = LogQueryParser.parse("field:durationMs > 250")
        val booleanExpression = LogQueryParser.parse("field:isRetry = true")
        val nullExpression = LogQueryParser.parse("field:error = null")
        val quotedNullExpression = LogQueryParser.parse("field:error = \"null\"")

        expectThat(numberExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "durationMs",
                operator = FieldOperator.GREATER_THAN,
                value = QueryLiteral.NumberValue(BigDecimal("250")),
                explicitFieldPrefix = true
            )
        )
        expectThat(booleanExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "isRetry",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.BooleanValue(true),
                explicitFieldPrefix = true
            )
        )
        expectThat(nullExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "error",
                operator = FieldOperator.IS_NULL,
                value = QueryLiteral.NullValue,
                explicitFieldPrefix = true
            )
        )
        expectThat(quotedNullExpression).isEqualTo(
            LogQueryExpression.FieldPredicate(
                path = "error",
                operator = FieldOperator.EQUALS,
                value = QueryLiteral.StringValue("null"),
                explicitFieldPrefix = true
            )
        )
    }

    @Test
    fun `parses boolean precedence with parentheses`() {
        val expression = LogQueryParser.parse("(level:error OR level:warn) AND message contains \"timeout\"")

        expectThat(expression).isEqualTo(
            LogQueryExpression.BooleanExpression(
                operator = BooleanOperator.AND,
                left = LogQueryExpression.BooleanExpression(
                    operator = BooleanOperator.OR,
                    left = LogQueryExpression.FieldPredicate(
                        path = "level",
                        operator = FieldOperator.EQUALS,
                        value = QueryLiteral.StringValue("error"),
                        explicitFieldPrefix = false
                    ),
                    right = LogQueryExpression.FieldPredicate(
                        path = "level",
                        operator = FieldOperator.EQUALS,
                        value = QueryLiteral.StringValue("warn"),
                        explicitFieldPrefix = false
                    )
                ),
                right = LogQueryExpression.FieldPredicate(
                    path = "message",
                    operator = FieldOperator.CONTAINS,
                    value = QueryLiteral.StringValue("timeout"),
                    explicitFieldPrefix = false
                )
            )
        )
    }

    @Test
    fun `falls back to text query for malformed or unsupported syntax`() {
        val malformedStructured = LogQueryParser.parse("field:TraceId >=")
        val malformedBacktickPath = LogQueryParser.parse("field:`Trace.Id=\"abc\"")
        val malformedIndexPath = LogQueryParser.parse("field:items[]=\"abc\"")
        val plainText = LogQueryParser.parse("timeout")

        expectThat(malformedStructured).isEqualTo(LogQueryExpression.TextQuery("field:TraceId >="))
        expectThat(malformedBacktickPath).isEqualTo(LogQueryExpression.TextQuery("field:`Trace.Id=\"abc\""))
        expectThat(malformedIndexPath).isEqualTo(LogQueryExpression.TextQuery("field:items[]=\"abc\""))
        expectThat(plainText).isEqualTo(LogQueryExpression.TextQuery("timeout"))
    }

    @Test
    fun `parses legacy dashboard field queries`() {
        val expression = LogQueryParser.parse("@field:service=auth")

        expectThat(expression).isEqualTo(
            LogQueryExpression.LegacyDashboardFieldQuery(
                field = "service",
                value = "auth"
            )
        )
    }
}