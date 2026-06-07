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
    private const val DASHBOARD_FIELD_QUERY_PREFIX = "@field:"
    private const val FIELD_PREFIX = "field:"
    private const val HAS_PREFIX = "has:"

    private val BOOLEAN_TRUE = "true"
    private val BOOLEAN_FALSE = "false"
    private val NULL_TOKEN = "null"

    private val NUMERIC_LITERAL_PATTERN = Regex("^-?\\d+(\\.\\d+)?$")
    private val CANONICAL_SHORT_FORM_PATTERN = Regex("^([A-Za-z_][A-Za-z0-9_.\\[\\]`\\-]*)\\s*:(.+)$")
    private val SYMBOL_OPERATOR_PATTERN = Regex("^(.+?)(>=|<=|=|~|>|<)(.+)$")
    private val CONTAINS_PATTERN = Regex("^(.+?)\\s+contains\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val EXISTS_MISSING_PATTERN = Regex("^(.+?)\\s+(exists|missing)$", RegexOption.IGNORE_CASE)
    private val IS_NULL_PATTERN = Regex("^(.+?)\\s+is\\s+null$", RegexOption.IGNORE_CASE)
    private val KNOWN_CANONICAL_ALIASES = setOf("level", "message", "trace.id")
    private val NON_VALUE_OPERATOR_TOKENS = setOf("=", "~", ">", ">=", "<", "<=")

    fun parse(query: String): LogQueryExpression {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return LogQueryExpression.TextQuery(query)
        }

        val tokens = tokenize(trimmed) ?: return LogQueryExpression.TextQuery(query)
        val parsedExpression = BooleanExpressionParser(tokens).parse()
        return parsedExpression ?: LogQueryExpression.TextQuery(query)
    }

    private fun parsePredicate(rawPredicate: String): LogQueryExpression? {
        val predicate = rawPredicate.trim()
        if (predicate.isEmpty()) {
            return null
        }

        parseLegacyDashboardField(predicate)?.let { return it }
        parseHasShortForm(predicate)?.let { return it }
        parseCanonicalShortForm(predicate)?.let { return it }
        parseIsNullPredicate(predicate)?.let { return it }
        parseExistsMissingPredicate(predicate)?.let { return it }
        parseContainsPredicate(predicate)?.let { return it }
        parseSymbolOperatorPredicate(predicate)?.let { return it }

        return null
    }

    private fun parseLegacyDashboardField(predicate: String): LogQueryExpression.LegacyDashboardFieldQuery? {
        if (!predicate.startsWith(DASHBOARD_FIELD_QUERY_PREFIX)) {
            return null
        }

        val payload = predicate.removePrefix(DASHBOARD_FIELD_QUERY_PREFIX)
        val delimiterIndex = payload.indexOf('=')
        if (delimiterIndex <= 0 || delimiterIndex == payload.lastIndex) {
            return null
        }

        val key = payload.substring(0, delimiterIndex).trim()
        val value = payload.substring(delimiterIndex + 1).trim()
        if (key.isEmpty() || value.isEmpty()) {
            return null
        }

        return LogQueryExpression.LegacyDashboardFieldQuery(field = key, value = value)
    }

    private fun parseHasShortForm(predicate: String): LogQueryExpression.FieldPredicate? {
        if (!predicate.startsWith(HAS_PREFIX, ignoreCase = true)) {
            return null
        }

        val path = predicate.substringAfter(':').trim()
        val parsedPath = StructuredQueryPath.parse(path) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = parsedPath.normalizedPath(),
            operator = FieldOperator.EXISTS,
            explicitFieldPrefix = false
        )
    }

    private fun parseCanonicalShortForm(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = CANONICAL_SHORT_FORM_PATTERN.matchEntire(predicate) ?: return null
        val rawPath = match.groupValues[1].trim()
        val rawValue = match.groupValues[2].trim()
        if (rawPath.equals(HAS_PREFIX.removeSuffix(":"), ignoreCase = true)) {
            return null
        }
        if (rawPath.equals(FIELD_PREFIX.removeSuffix(":"), ignoreCase = true)) {
            return null
        }
        if (rawValue.isEmpty()) {
            return null
        }

        val parsedPath = StructuredQueryPath.parse(rawPath) ?: return null
        val normalizedPath = parsedPath.normalizedPath()
        if (!isCanonicalPathCandidate(rawPath = rawPath, normalizedPath = normalizedPath)) {
            return null
        }

        val literal = parseLiteral(rawValue) ?: return null
        return LogQueryExpression.FieldPredicate(
            path = normalizedPath,
            operator = FieldOperator.EQUALS,
            value = literal,
            explicitFieldPrefix = false
        )
    }

    private fun parseContainsPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = CONTAINS_PATTERN.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null
        val literal = parseLiteral(match.groupValues[2]) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = FieldOperator.CONTAINS,
            value = literal,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parseExistsMissingPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = EXISTS_MISSING_PATTERN.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null
        val operator = when (match.groupValues[2].lowercase()) {
            "exists" -> FieldOperator.EXISTS
            "missing" -> FieldOperator.MISSING
            else -> return null
        }

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = operator,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parseIsNullPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = IS_NULL_PATTERN.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = FieldOperator.IS_NULL,
            value = QueryLiteral.NullValue,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parseSymbolOperatorPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = SYMBOL_OPERATOR_PATTERN.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null
        val operator = when (match.groupValues[2].trim()) {
            "=" -> FieldOperator.EQUALS
            "~" -> FieldOperator.REGEX
            ">" -> FieldOperator.GREATER_THAN
            ">=" -> FieldOperator.GREATER_THAN_OR_EQUAL
            "<" -> FieldOperator.LESS_THAN
            "<=" -> FieldOperator.LESS_THAN_OR_EQUAL
            else -> return null
        }
        val rawLiteral = match.groupValues[3].trim()
        if (rawLiteral.isEmpty() || NON_VALUE_OPERATOR_TOKENS.contains(rawLiteral)) {
            return null
        }
        val literal = parseLiteral(rawLiteral) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = if (literal is QueryLiteral.NullValue && operator == FieldOperator.EQUALS) {
                FieldOperator.IS_NULL
            } else {
                operator
            },
            value = literal,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parsePathToken(rawPathToken: String): ParsedPathToken? {
        val trimmed = rawPathToken.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        if (trimmed.startsWith(FIELD_PREFIX, ignoreCase = true)) {
            val explicitPath = trimmed.substringAfter(':').trim()
            val parsedPath = StructuredQueryPath.parse(explicitPath) ?: return null
            return ParsedPathToken(
                path = parsedPath.normalizedPath(),
                explicitFieldPrefix = true
            )
        }

        val parsedPath = StructuredQueryPath.parse(trimmed) ?: return null
        val normalizedPath = parsedPath.normalizedPath()
        if (!isCanonicalPathCandidate(rawPath = trimmed, normalizedPath = normalizedPath)) {
            return null
        }

        return ParsedPathToken(path = normalizedPath, explicitFieldPrefix = false)
    }

    private fun isCanonicalPathCandidate(rawPath: String, normalizedPath: String): Boolean {
        if (normalizedPath.isBlank()) {
            return false
        }

        val lowered = normalizedPath.lowercase()
        return KNOWN_CANONICAL_ALIASES.contains(lowered) ||
            rawPath.contains('.') ||
            rawPath.contains('[') ||
            rawPath.contains('`') ||
            rawPath.contains('\\')
    }

    private fun parseLiteral(rawLiteral: String): QueryLiteral? {
        val literal = rawLiteral.trim()
        if (literal.isEmpty()) {
            return null
        }

        if (literal.startsWith('"') || literal.endsWith('"')) {
            val parsedString = parseQuotedString(literal) ?: return null
            return QueryLiteral.StringValue(parsedString)
        }

        val lowered = literal.lowercase()
        if (lowered == BOOLEAN_TRUE) {
            return QueryLiteral.BooleanValue(true)
        }
        if (lowered == BOOLEAN_FALSE) {
            return QueryLiteral.BooleanValue(false)
        }
        if (lowered == NULL_TOKEN) {
            return QueryLiteral.NullValue
        }

        if (NUMERIC_LITERAL_PATTERN.matches(literal)) {
            return runCatching { QueryLiteral.NumberValue(BigDecimal(literal)) }
                .getOrElse { QueryLiteral.StringValue(literal) }
        }

        return QueryLiteral.StringValue(literal)
    }

    private fun parseQuotedString(quotedLiteral: String): String? {
        if (quotedLiteral.length < 2 || quotedLiteral.first() != '"' || quotedLiteral.last() != '"') {
            return null
        }

        val unescaped = StringBuilder()
        var index = 1
        val lastIndex = quotedLiteral.lastIndex
        while (index < lastIndex) {
            val character = quotedLiteral[index]
            if (character == '\\') {
                if (index + 1 >= lastIndex) {
                    return null
                }
                val escaped = quotedLiteral[index + 1]
                val unescapedCharacter = when (escaped) {
                    '"' -> '"'
                    '\\' -> '\\'
                    'n' -> '\n'
                    'r' -> '\r'
                    't' -> '\t'
                    else -> escaped
                }
                unescaped.append(unescapedCharacter)
                index += 2
            } else {
                unescaped.append(character)
                index += 1
            }
        }

        return unescaped.toString()
    }

    private fun tokenize(query: String): List<QueryToken>? {
        val tokens = mutableListOf<QueryToken>()
        var index = 0

        while (index < query.length) {
            val character = query[index]
            when {
                character.isWhitespace() -> {
                    index += 1
                }

                character == '(' -> {
                    tokens += QueryToken(type = QueryTokenType.LEFT_PARENTHESIS, lexeme = "(")
                    index += 1
                }

                character == ')' -> {
                    tokens += QueryToken(type = QueryTokenType.RIGHT_PARENTHESIS, lexeme = ")")
                    index += 1
                }

                character == '"' -> {
                    val start = index
                    index += 1
                    var isClosed = false
                    while (index < query.length) {
                        val currentCharacter = query[index]
                        if (currentCharacter == '\\') {
                            index += 2
                        } else if (currentCharacter == '"') {
                            index += 1
                            isClosed = true
                            break
                        } else {
                            index += 1
                        }
                    }

                    if (!isClosed) {
                        return null
                    }

                    tokens += QueryToken(
                        type = QueryTokenType.ATOM,
                        lexeme = query.substring(start, index)
                    )
                }

                else -> {
                    val start = index
                    while (index < query.length && !query[index].isWhitespace() && query[index] != '(' && query[index] != ')') {
                        index += 1
                    }
                    tokens += QueryToken(
                        type = QueryTokenType.ATOM,
                        lexeme = query.substring(start, index)
                    )
                }
            }
        }

        tokens += QueryToken(type = QueryTokenType.EOF, lexeme = "")
        return tokens
    }

    private class BooleanExpressionParser(
        tokens: List<QueryToken>
    ) {
        private val tokenStream = QueryTokenStream(tokens)

        fun parse(): LogQueryExpression? {
            val expression = parseOrExpression() ?: return null
            if (!tokenStream.isAtEnd()) {
                return null
            }
            return expression
        }

        private fun parseOrExpression(): LogQueryExpression? {
            var expression = parseAndExpression() ?: return null
            while (tokenStream.matchKeyword("OR")) {
                val rightExpression = parseAndExpression() ?: return null
                expression = LogQueryExpression.BooleanExpression(
                    operator = BooleanOperator.OR,
                    left = expression,
                    right = rightExpression
                )
            }

            return expression
        }

        private fun parseAndExpression(): LogQueryExpression? {
            var expression = parsePrimaryExpression() ?: return null
            while (tokenStream.matchKeyword("AND")) {
                val rightExpression = parsePrimaryExpression() ?: return null
                expression = LogQueryExpression.BooleanExpression(
                    operator = BooleanOperator.AND,
                    left = expression,
                    right = rightExpression
                )
            }

            return expression
        }

        private fun parsePrimaryExpression(): LogQueryExpression? {
            if (tokenStream.matchType(QueryTokenType.LEFT_PARENTHESIS)) {
                val expression = parseOrExpression() ?: return null
                if (!tokenStream.matchType(QueryTokenType.RIGHT_PARENTHESIS)) {
                    return null
                }
                return expression
            }

            val predicateTokens = mutableListOf<QueryToken>()
            while (
                !tokenStream.isAtEnd() &&
                tokenStream.peekType() != QueryTokenType.RIGHT_PARENTHESIS &&
                !tokenStream.peekKeyword("AND") &&
                !tokenStream.peekKeyword("OR")
            ) {
                predicateTokens += tokenStream.advance()
            }

            if (predicateTokens.isEmpty()) {
                return null
            }

            val predicate = predicateTokens.joinToString(separator = " ") { token -> token.lexeme }
            return parsePredicate(predicate)
        }
    }

    private class QueryTokenStream(
        private val tokens: List<QueryToken>
    ) {
        private var cursor: Int = 0

        fun advance(): QueryToken {
            val current = tokens[cursor]
            cursor += 1
            return current
        }

        fun matchType(type: QueryTokenType): Boolean {
            if (peekType() != type) {
                return false
            }
            advance()
            return true
        }

        fun matchKeyword(keyword: String): Boolean {
            if (!peekKeyword(keyword)) {
                return false
            }
            advance()
            return true
        }

        fun peekKeyword(keyword: String): Boolean {
            val token = tokens[cursor]
            return token.type == QueryTokenType.ATOM && token.lexeme.equals(keyword, ignoreCase = true)
        }

        fun peekType(): QueryTokenType {
            return tokens[cursor].type
        }

        fun isAtEnd(): Boolean {
            return peekType() == QueryTokenType.EOF
        }
    }

    private data class ParsedPathToken(
        val path: String,
        val explicitFieldPrefix: Boolean
    )

    private data class QueryToken(
        val type: QueryTokenType,
        val lexeme: String
    )

    private enum class QueryTokenType {
        ATOM,
        LEFT_PARENTHESIS,
        RIGHT_PARENTHESIS,
        EOF
    }
}