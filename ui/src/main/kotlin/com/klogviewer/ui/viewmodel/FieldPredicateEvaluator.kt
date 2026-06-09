package com.klogviewer.ui.viewmodel

internal class FieldPredicateEvaluator {
    private val strategies: Map<FieldOperator, FieldOperatorStrategy> = listOf(
        ExistsOperatorStrategy,
        MissingOperatorStrategy,
        IsNullOperatorStrategy,
        EqualsOperatorStrategy,
        ContainsOperatorStrategy,
        RegexOperatorStrategy,
        GreaterThanOperatorStrategy,
        GreaterThanOrEqualOperatorStrategy,
        LessThanOperatorStrategy,
        LessThanOrEqualOperatorStrategy
    ).associateBy(FieldOperatorStrategy::operator)

    fun matches(
        predicate: LogQueryExpression.FieldPredicate,
        fieldValues: List<ResolvedFieldValue>,
        fieldExists: Boolean
    ): Boolean {
        val strategy = strategies[predicate.operator] ?: return false
        return strategy.matches(
            context = OperatorEvaluationContext(
                predicate = predicate,
                fieldValues = fieldValues,
                fieldExists = fieldExists
            )
        )
    }
}

internal data class OperatorEvaluationContext(
    val predicate: LogQueryExpression.FieldPredicate,
    val fieldValues: List<ResolvedFieldValue>,
    val fieldExists: Boolean
)

internal sealed interface FieldOperatorStrategy {
    val operator: FieldOperator

    fun matches(context: OperatorEvaluationContext): Boolean
}

private data object ExistsOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.EXISTS

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return context.fieldExists
    }
}

private data object MissingOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.MISSING

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return !context.fieldExists
    }
}

private data object IsNullOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.IS_NULL

    override fun matches(context: OperatorEvaluationContext): Boolean {
        // NOTE: null-vs-missing is precise for structured values; compatibility string projections may
        // collapse unknowns to text and are treated as best-effort `"null"` matching.
        return context.fieldValues.any { value -> value is ResolvedFieldValue.NullValue }
    }
}

private data object EqualsOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.EQUALS

    override fun matches(context: OperatorEvaluationContext): Boolean {
        val literal = context.predicate.value ?: return false
        return when (literal) {
            is QueryLiteral.StringValue -> {
                if (isCaseInsensitiveLevelMatch(context.predicate)) {
                    context.fieldValues.any { value -> value.toSearchToken().equals(literal.value, ignoreCase = true) }
                } else {
                    context.fieldValues.any { value -> value.toSearchToken() == literal.value }
                }
            }

            is QueryLiteral.NumberValue -> {
                context.fieldValues.any { value -> value.toComparableNumber() == literal.value }
            }

            is QueryLiteral.BooleanValue -> {
                context.fieldValues.any { value -> value.toComparableBoolean() == literal.value }
            }

            QueryLiteral.NullValue -> {
                context.fieldValues.any { value -> value is ResolvedFieldValue.NullValue }
            }
        }
    }

    private fun isCaseInsensitiveLevelMatch(predicate: LogQueryExpression.FieldPredicate): Boolean {
        return !predicate.explicitFieldPrefix && predicate.path.equals("level", ignoreCase = true)
    }
}

private data object ContainsOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.CONTAINS

    override fun matches(context: OperatorEvaluationContext): Boolean {
        val literal = context.predicate.value?.toSearchToken() ?: return false
        return context.fieldValues.any { value ->
            value.toSearchToken().contains(literal, ignoreCase = true)
        }
    }
}

private data object RegexOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.REGEX

    @Suppress("ReturnCount")
    override fun matches(context: OperatorEvaluationContext): Boolean {
        val pattern = context.predicate.value?.toSearchToken() ?: return false
        val regex = runCatching {
            Regex(pattern, setOf(RegexOption.IGNORE_CASE))
        }.getOrNull() ?: return false

        return context.fieldValues.any { value -> regex.containsMatchIn(value.toSearchToken()) }
    }
}

private data object GreaterThanOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.GREATER_THAN

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return compareNumbers(context) { fieldNumber, literalNumber -> fieldNumber > literalNumber }
    }
}

private data object GreaterThanOrEqualOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.GREATER_THAN_OR_EQUAL

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return compareNumbers(context) { fieldNumber, literalNumber -> fieldNumber >= literalNumber }
    }
}

private data object LessThanOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.LESS_THAN

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return compareNumbers(context) { fieldNumber, literalNumber -> fieldNumber < literalNumber }
    }
}

private data object LessThanOrEqualOperatorStrategy : FieldOperatorStrategy {
    override val operator: FieldOperator = FieldOperator.LESS_THAN_OR_EQUAL

    override fun matches(context: OperatorEvaluationContext): Boolean {
        return compareNumbers(context) { fieldNumber, literalNumber -> fieldNumber <= literalNumber }
    }
}

private inline fun compareNumbers(
    context: OperatorEvaluationContext,
    compare: (fieldNumber: java.math.BigDecimal, literalNumber: java.math.BigDecimal) -> Boolean
): Boolean {
    val literalNumber = context.predicate.value.toComparableNumber() ?: return false
    return context.fieldValues.any { value ->
        val fieldNumber = value.toComparableNumber() ?: return@any false
        compare(fieldNumber, literalNumber)
    }
}
