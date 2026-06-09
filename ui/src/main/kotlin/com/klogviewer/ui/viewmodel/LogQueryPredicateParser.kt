package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.CanonicalFieldAliases

private const val PATH_GROUP_INDEX = 1
private const val OPERATOR_GROUP_INDEX = 2
private const val VALUE_GROUP_INDEX = 3

internal class LogQueryPredicateParser(
    private val literalParser: LogQueryLiteralParser = LogQueryLiteralParser()
) {
    private val canonicalShortFormPattern = Regex("^([A-Za-z_][A-Za-z0-9_.\\[\\]`\\-]*)\\s*:(.+)$")
    private val symbolOperatorPattern = Regex("^(.+?)(>=|<=|=|~|>|<)(.+)$")
    private val eqOperatorPattern = Regex("^(.+?)\\s+eq\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val containsPattern = Regex("^(.+?)\\s+contains\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val existsMissingPattern = Regex("^(.+?)\\s+(exists|missing)$", RegexOption.IGNORE_CASE)
    private val isNullPattern = Regex("^(.+?)\\s+is\\s+null$", RegexOption.IGNORE_CASE)
    private val knownCanonicalAliases = CanonicalFieldAliases.QUERY_SHORT_FORM_CANONICAL_KEYS
    private val nonValueOperatorTokens = setOf("=", "~", ">", ">=", "<", "<=", "eq")

    fun parse(rawPredicate: String): LogQueryExpression? {
        val predicate = rawPredicate.trim()
        if (predicate.isEmpty()) {
            return null
        }

        val parsers = listOf<(String) -> LogQueryExpression?>(
            ::parseLegacyDashboardField,
            ::parseHasShortForm,
            ::parseCanonicalShortForm,
            ::parseIsNullPredicate,
            ::parseExistsMissingPredicate,
            ::parseContainsPredicate,
            ::parseSymbolOperatorPredicate
        )
        return parsers.firstNotNullOfOrNull { parser -> parser(predicate) }
    }

    private fun parseLegacyDashboardField(predicate: String): LogQueryExpression.LegacyDashboardFieldQuery? {
        if (!predicate.startsWith(DASHBOARD_FIELD_QUERY_PREFIX)) {
            return null
        }

        val payload = predicate.removePrefix(DASHBOARD_FIELD_QUERY_PREFIX)
        val delimiterIndex = payload.indexOf('=')
        val key = payload.substring(0, delimiterIndex).trim()
        val value = payload.substring(delimiterIndex + 1).trim()
        val isValid = delimiterIndex > 0 && delimiterIndex < payload.lastIndex && key.isNotEmpty() && value.isNotEmpty()
        return if (isValid) {
            LogQueryExpression.LegacyDashboardFieldQuery(field = key, value = value)
        } else {
            null
        }
    }

    private fun parseHasShortForm(predicate: String): LogQueryExpression.FieldPredicate? {
        if (!predicate.startsWith(HAS_PREFIX, ignoreCase = true)) {
            return null
        }

        val path = predicate.substringAfter(':').trim()
        val normalizedPath = StructuredQueryPath.parse(path)?.normalizedPath()
        return normalizedPath?.let { parsedPath ->
            LogQueryExpression.FieldPredicate(
                path = parsedPath,
                operator = FieldOperator.EXISTS,
                explicitFieldPrefix = false
            )
        }
    }

    private fun parseCanonicalShortForm(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = canonicalShortFormPattern.matchEntire(predicate) ?: return null
        val rawPath = match.groupValues[PATH_GROUP_INDEX].trim()
        val rawValue = match.groupValues[OPERATOR_GROUP_INDEX].trim()
        val isReservedPath = rawPath.equals(HAS_PREFIX.removeSuffix(":"), ignoreCase = true) ||
            rawPath.equals(FIELD_PREFIX.removeSuffix(":"), ignoreCase = true)
        val normalizedPath = StructuredQueryPath.parse(rawPath)?.normalizedPath()
        val isCanonicalPath = normalizedPath != null &&
            !isReservedPath &&
            rawValue.isNotEmpty() &&
            isCanonicalPathCandidate(rawPath = rawPath, normalizedPath = normalizedPath)
        val literal = if (isCanonicalPath) literalParser.parse(rawValue) else null

        return if (normalizedPath != null && literal != null && isCanonicalPath) {
            LogQueryExpression.FieldPredicate(
                path = normalizedPath,
                operator = FieldOperator.EQUALS,
                value = literal,
                explicitFieldPrefix = false
            )
        } else {
            null
        }
    }

    private fun parseContainsPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = containsPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[PATH_GROUP_INDEX])
        val literal = literalParser.parse(match.groupValues[OPERATOR_GROUP_INDEX])

        return if (pathToken != null && literal != null) {
            LogQueryExpression.FieldPredicate(
                path = pathToken.path,
                operator = FieldOperator.CONTAINS,
                value = literal,
                explicitFieldPrefix = pathToken.explicitFieldPrefix
            )
        } else {
            null
        }
    }

    private fun parseExistsMissingPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = existsMissingPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[PATH_GROUP_INDEX])
        val operator = when (match.groupValues[OPERATOR_GROUP_INDEX].lowercase()) {
            "exists" -> FieldOperator.EXISTS
            "missing" -> FieldOperator.MISSING
            else -> null
        }

        return if (pathToken != null && operator != null) {
            LogQueryExpression.FieldPredicate(
                path = pathToken.path,
                operator = operator,
                explicitFieldPrefix = pathToken.explicitFieldPrefix
            )
        } else {
            null
        }
    }

    private fun parseIsNullPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val match = isNullPattern.matchEntire(predicate) ?: return null
        val pathToken = parsePathToken(match.groupValues[PATH_GROUP_INDEX])

        return pathToken?.let { parsedPathToken ->
            LogQueryExpression.FieldPredicate(
                path = parsedPathToken.path,
                operator = FieldOperator.IS_NULL,
                value = QueryLiteral.NullValue,
                explicitFieldPrefix = parsedPathToken.explicitFieldPrefix
            )
        }
    }

    private fun parseSymbolOperatorPredicate(predicate: String): LogQueryExpression.FieldPredicate? {
        val operatorParts = parseOperatorParts(predicate) ?: return null
        val pathToken = parsePathToken(operatorParts.first)
        val rawLiteral = operatorParts.third
        val literal = if (rawLiteral.isEmpty() || nonValueOperatorTokens.contains(rawLiteral)) {
            null
        } else {
            literalParser.parse(rawLiteral)
        }
        val shouldUseIsNullOperator =
            literal is QueryLiteral.NullValue && operatorParts.second == FieldOperator.EQUALS
        val normalizedOperator = if (shouldUseIsNullOperator) {
            FieldOperator.IS_NULL
        } else {
            operatorParts.second
        }

        return if (pathToken != null && literal != null) {
            LogQueryExpression.FieldPredicate(
                path = pathToken.path,
                operator = normalizedOperator,
                value = literal,
                explicitFieldPrefix = pathToken.explicitFieldPrefix
            )
        } else {
            null
        }
    }

    private fun parseOperatorParts(predicate: String): Triple<String, FieldOperator, String>? {
        val eqMatch = eqOperatorPattern.matchEntire(predicate)
        if (eqMatch != null) {
            return Triple(
                eqMatch.groupValues[PATH_GROUP_INDEX],
                FieldOperator.EQUALS,
                eqMatch.groupValues[OPERATOR_GROUP_INDEX].trim()
            )
        }

        val symbolMatch = symbolOperatorPattern.matchEntire(predicate)
        val pathToken = symbolMatch?.groupValues?.get(PATH_GROUP_INDEX)
        val operator = symbolMatch
            ?.groupValues
            ?.get(OPERATOR_GROUP_INDEX)
            ?.trim()
            ?.let(::parseFieldOperator)
        val literal = symbolMatch?.groupValues?.get(VALUE_GROUP_INDEX)?.trim()
        return if (pathToken != null && operator != null && literal != null) {
            Triple(pathToken, operator, literal)
        } else {
            null
        }
    }

    private fun parsePathToken(rawPathToken: String): ParsedPathToken? {
        val trimmed = rawPathToken.trim()
        if (trimmed.isEmpty()) {
            return null
        }

        val parsedPathToken = if (trimmed.startsWith(FIELD_PREFIX, ignoreCase = true)) {
            val explicitPath = trimmed.substringAfter(':').trim()
            StructuredQueryPath.parse(explicitPath)
                ?.normalizedPath()
                ?.let { path ->
                    ParsedPathToken(
                        path = path,
                        explicitFieldPrefix = true
                    )
                }
        } else {
            StructuredQueryPath.parse(trimmed)
                ?.normalizedPath()
                ?.takeIf { normalizedPath ->
                    isCanonicalPathCandidate(rawPath = trimmed, normalizedPath = normalizedPath)
                }
                ?.let { normalizedPath ->
                    ParsedPathToken(path = normalizedPath, explicitFieldPrefix = false)
                }
        }
        return parsedPathToken
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

private fun parseFieldOperator(token: String): FieldOperator? {
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
