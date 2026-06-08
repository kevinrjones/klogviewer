package com.klogviewer.ui.viewmodel

import java.math.BigDecimal

internal class LogQueryLiteralParser {
    private val numericLiteralPattern = Regex("^-?\\d+(\\.\\d+)?$")

    fun parse(rawLiteral: String): QueryLiteral? {
        val literal = rawLiteral.trim()
        if (literal.isEmpty()) {
            return null
        }

        if (literal.startsWith('"') || literal.endsWith('"')) {
            val parsedString = parseQuotedString(literal) ?: return null
            return QueryLiteral.StringValue(parsedString)
        }

        return when (literal.lowercase()) {
            "true" -> QueryLiteral.BooleanValue(true)
            "false" -> QueryLiteral.BooleanValue(false)
            "null" -> QueryLiteral.NullValue
            else -> parseNumberOrString(literal)
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
        while (index < lastIndex) {
            val character = quotedLiteral[index]
            if (character == '\\') {
                if (index + 1 >= lastIndex) {
                    return null
                }
                unescaped.append(unescapeCharacter(quotedLiteral[index + 1]))
                index += 2
                continue
            }

            unescaped.append(character)
            index += 1
        }

        return unescaped.toString()
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
