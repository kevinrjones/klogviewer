package com.klogviewer.ui.viewmodel

internal class LogQueryExpressionParser(
    private val tokenizer: LogQueryTokenizer = LogQueryTokenizer(),
    private val predicateParser: LogQueryPredicateParser = LogQueryPredicateParser()
) {
    fun parse(query: String): LogQueryExpression? {
        return tokenizer
            .tokenize(query)
            ?.let { tokens ->
                BooleanExpressionParser(tokens = tokens, predicateParser = predicateParser).parse()
            }
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
        while (index < query.length && isAtomCharacter(query[index])) {
            index += 1
        }
        return index
    }

    private fun isAtomCharacter(character: Char): Boolean {
        return !character.isWhitespace() && character != '(' && character != ')'
    }
}

internal class BooleanExpressionParser(
    tokens: List<QueryToken>,
    private val predicateParser: LogQueryPredicateParser
) {
    private val tokenStream = QueryTokenStream(tokens)

    fun parse(): LogQueryExpression? {
        return parseOrExpression()?.takeIf { tokenStream.isAtEnd() }
    }

    private fun parseOrExpression(): LogQueryExpression? {
        return parseBinaryExpression(
            keyword = "OR",
            operator = BooleanOperator.OR,
            parseOperand = ::parseAndExpression
        )
    }

    private fun parseAndExpression(): LogQueryExpression? {
        return parseBinaryExpression(
            keyword = "AND",
            operator = BooleanOperator.AND,
            parseOperand = ::parsePrimaryExpression
        )
    }

    private fun parsePrimaryExpression(): LogQueryExpression? {
        return if (tokenStream.matchType(QueryTokenType.LEFT_PARENTHESIS)) {
            parseParenthesizedExpression()
        } else {
            parsePredicateExpression()
        }
    }

    private fun parseBinaryExpression(
        keyword: String,
        operator: BooleanOperator,
        parseOperand: () -> LogQueryExpression?
    ): LogQueryExpression? {
        var expression = parseOperand()
        var malformed = expression == null

        while (!malformed && tokenStream.matchKeyword(keyword)) {
            val rightExpression = parseOperand()
            if (rightExpression == null || expression == null) {
                malformed = true
            } else {
                expression = LogQueryExpression.BooleanExpression(
                    operator = operator,
                    left = expression,
                    right = rightExpression
                )
            }
        }

        return if (malformed) null else expression
    }

    private fun parseParenthesizedExpression(): LogQueryExpression? {
        val expression = parseOrExpression()
        val isClosed = tokenStream.matchType(QueryTokenType.RIGHT_PARENTHESIS)
        return if (expression != null && isClosed) expression else null
    }

    private fun parsePredicateExpression(): LogQueryExpression? {
        val predicateTokens = mutableListOf<QueryToken>()
        while (!isExpressionTerminator()) {
            predicateTokens += tokenStream.advance()
        }

        val predicate = predicateTokens
            .takeIf { tokens -> tokens.isNotEmpty() }
            ?.joinToString(separator = " ") { token -> token.lexeme }
        return predicate?.let(predicateParser::parse)
    }

    private fun isExpressionTerminator(): Boolean {
        val isRightParenthesis = tokenStream.peekType() == QueryTokenType.RIGHT_PARENTHESIS
        val isLogicalKeyword = tokenStream.peekKeyword("AND") || tokenStream.peekKeyword("OR")
        return tokenStream.isAtEnd() || isRightParenthesis || isLogicalKeyword
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
