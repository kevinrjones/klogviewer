package com.klogviewer.ui.viewmodel

import java.math.BigDecimal

internal class LogQueryLiteralParser {
    private val numericLiteralPattern = Regex("^-?\\d+(\\.\\d+)?$")

    fun parse(rawLiteral: String): QueryLiteral? {
        val literal = rawLiteral.trim()
        if (literal.isEmpty()) {
            return null
        }

        return if (literal.startsWith('"') || literal.endsWith('"')) {
            parseQuotedString(literal)?.let(QueryLiteral::StringValue)
        } else {
            when (literal.lowercase()) {
            "true" -> QueryLiteral.BooleanValue(true)
            "false" -> QueryLiteral.BooleanValue(false)
            "null" -> QueryLiteral.NullValue
            else -> parseNumberOrString(literal)
            }
        }
    }

    private fun parseNumberOrString(literal: String): QueryLiteral {
        if (!numericLiteralPattern.matches(literal)) {
            return QueryLiteral.StringValue(literal)
        }

        return runCatching { QueryLiteral.NumberValue(BigDecimal(literal)) }
            .getOrElse { QueryLiteral.StringValue(literal) }
    }

    private fun parseQuotedString(quotedLiteral: String): String? {
        if (quotedLiteral.length < 2 || quotedLiteral.first() != '"' || quotedLiteral.last() != '"') {
            return null
        }

        val unescaped = StringBuilder()
        var index = 1
        val lastIndex = quotedLiteral.lastIndex
        var malformed = false
        while (index < lastIndex) {
            val character = quotedLiteral[index]
            val isEscapeSequence = character == '\\'
            if (isEscapeSequence && index + 1 >= lastIndex) {
                malformed = true
                break
            }

            if (isEscapeSequence) {
                unescaped.append(unescapeCharacter(quotedLiteral[index + 1]))
                index += 2
            } else {
                unescaped.append(character)
                index += 1
            }
        }

        return if (malformed) null else unescaped.toString()
    }

    private fun unescapeCharacter(escaped: Char): Char {
        return when (escaped) {
            '"' -> '"'
            '\\' -> '\\'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            else -> escaped
        }
    }
}
