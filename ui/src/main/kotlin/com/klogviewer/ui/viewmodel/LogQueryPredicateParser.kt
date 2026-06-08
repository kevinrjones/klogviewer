package com.klogviewer.ui.viewmodel

internal class LogQueryPredicateParser(
    private val literalParser: LogQueryLiteralParser = LogQueryLiteralParser()
) {
    private val canonicalShortFormPattern = Regex("^([A-Za-z_][A-Za-z0-9_.\\[\\]`\\-]*)\\s*:(.+)$")
    private val symbolOperatorPattern = Regex("^(.+?)(>=|<=|=|~|>|<)(.+)$")
    private val containsPattern = Regex("^(.+?)\\s+contains\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val existsMissingPattern = Regex("^(.+?)\\s+(exists|missing)$", RegexOption.IGNORE_CASE)
    private val isNullPattern = Regex("^(.+?)\\s+is\\s+null$", RegexOption.IGNORE_CASE)
    private val knownCanonicalAliases = setOf("level", "message", "trace.id")
    private val nonValueOperatorTokens = setOf("=", "~", ">", ">=", "<", "<=")

    fun parse(rawPredicate: String): LogQueryExpression? {
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
        val match = canonicalShortFormPattern.matchEntire(predicate) ?: return null
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

        val literal = literalParser.parse(rawValue) ?: return null
        return LogQueryExpression.FieldPredicate(
            path = normalizedPath,
            operator = FieldOperator.EQUALS,
            value = literal,
            explicitFieldPrefix = false
        )
    }

    private fun parseContainsPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = containsPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null
        val literal = literalParser.parse(match.groupValues[2]) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = FieldOperator.CONTAINS,
            value = literal,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parseExistsMissingPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = existsMissingPattern.matchEntire(predicate) ?: return null
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
        val match = isNullPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null

        return LogQueryExpression.FieldPredicate(
            path = pathToken.path,
            operator = FieldOperator.IS_NULL,
            value = QueryLiteral.NullValue,
            explicitFieldPrefix = pathToken.explicitFieldPrefix
        )
    }

    private fun parseSymbolOperatorPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = symbolOperatorPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[1]) ?: return null
        val operator = parseSymbolOperator(match.groupValues[2].trim()) ?: return null
        val rawLiteral = match.groupValues[3].trim()
        if (rawLiteral.isEmpty() || nonValueOperatorTokens.contains(rawLiteral)) {
            return null
        }
        val literal = literalParser.parse(rawLiteral) ?: return null

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

    private fun parseSymbolOperator(token: String): FieldOperator? {
        return when (token) {
            "=" -> FieldOperator.EQUALS
            "~" -> FieldOperator.REGEX
            ">" -> FieldOperator.GREATER_THAN
            ">=" -> FieldOperator.GREATER_THAN_OR_EQUAL
            "<" -> FieldOperator.LESS_THAN
            "<=" -> FieldOperator.LESS_THAN_OR_EQUAL
            else -> null
        }
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
        return knownCanonicalAliases.contains(lowered) ||
            rawPath.contains('.') ||
            rawPath.contains('[') ||
            rawPath.contains('`') ||
            rawPath.contains('\\')
    }

    private data class ParsedPathToken(
        val path: String,
        val explicitFieldPrefix: Boolean
    )

    private companion object {
        private const val DASHBOARD_FIELD_QUERY_PREFIX = "@field:"
        private const val FIELD_PREFIX = "field:"
        private const val HAS_PREFIX = "has:"
    }
}
