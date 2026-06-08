package com.klogviewer.ui.viewmodel

internal class LogQueryExpressionParser(
    private val tokenizer: LogQueryTokenizer = LogQueryTokenizer(),
    private val predicateParser: LogQueryPredicateParser = LogQueryPredicateParser()
) {
    fun parse(query: String): LogQueryExpression? {
        val tokens = tokenizer.tokenize(query) ?: return null
        return BooleanExpressionParser(tokens = tokens, predicateParser = predicateParser).parse()
    }
}

internal class LogQueryTokenizer {
    fun tokenize(query: String): List<QueryToken>? {
        val tokens = mutableListOf<QueryToken>()
        var index = 0

        while (index < query.length) {
            when (val character = query[index]) {
                ' ' -> index += 1
                '\t' -> index += 1
                '\n' -> index += 1
                '\r' -> index += 1
                '(' -> {
                    tokens += QueryToken(type = QueryTokenType.LEFT_PARENTHESIS, lexeme = "(")
                    index += 1
                }

                ')' -> {
                    tokens += QueryToken(type = QueryTokenType.RIGHT_PARENTHESIS, lexeme = ")")
                    index += 1
                }

                '"' -> {
                    val quotedTokenEnd = findQuotedTokenEnd(query = query, quoteStart = index) ?: return null
                    tokens += QueryToken(
                        type = QueryTokenType.ATOM,
                        lexeme = query.substring(index, quotedTokenEnd)
                    )
                    index = quotedTokenEnd
                }

                else -> {
                    val atomEnd = findAtomTokenEnd(query = query, atomStart = index)
                    tokens += QueryToken(
                        type = QueryTokenType.ATOM,
                        lexeme = query.substring(index, atomEnd)
                    )
                    index = atomEnd
                }
            }
        }

        tokens += QueryToken(type = QueryTokenType.EOF, lexeme = "")
        return tokens
    }

    private fun findQuotedTokenEnd(query: String, quoteStart: Int): Int? {
        var index = quoteStart + 1
        while (index < query.length) {
            val currentCharacter = query[index]
            if (currentCharacter == '\\') {
                index += 2
            } else if (currentCharacter == '"') {
                return index + 1
            } else {
                index += 1
            }
        }

        return null
    }

    private fun findAtomTokenEnd(query: String, atomStart: Int): Int {
        var index = atomStart
        while (index < query.length && !query[index].isWhitespace() && query[index] != '(' && query[index] != ')') {
            index += 1
        }
        return index
    }
}

internal class BooleanExpressionParser(
    tokens: List<QueryToken>,
    private val predicateParser: LogQueryPredicateParser
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
        return predicateParser.parse(predicate)
    }
}

internal class QueryTokenStream(
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

internal data class QueryToken(
    val type: QueryTokenType,
    val lexeme: String
)

internal enum class QueryTokenType {
    ATOM,
    LEFT_PARENTHESIS,
    RIGHT_PARENTHESIS,
    EOF
}
