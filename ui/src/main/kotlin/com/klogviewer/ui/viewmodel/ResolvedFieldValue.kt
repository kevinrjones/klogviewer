package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.domain.model.asDisplayString
import java.math.BigDecimal

internal sealed interface ResolvedFieldValue {
    data class StringValue(val value: String) : ResolvedFieldValue
    data class NumberValue(val value: BigDecimal) : ResolvedFieldValue
    data class BooleanValue(val value: Boolean) : ResolvedFieldValue
    data object NullValue : ResolvedFieldValue
}

internal fun StructuredValue.toResolvedFieldValue(numericLiteralPattern: Regex): ResolvedFieldValue {
    return when (this) {
        is StructuredValue.StringValue -> ResolvedFieldValue.StringValue(value)
        is StructuredValue.NumberValue -> {
            if (numericLiteralPattern.matches(value)) {
                runCatching { ResolvedFieldValue.NumberValue(BigDecimal(value)) }
                    .getOrElse { ResolvedFieldValue.StringValue(value) }
            } else {
                ResolvedFieldValue.StringValue(value)
            }
        }

        is StructuredValue.BooleanValue -> ResolvedFieldValue.BooleanValue(value)
        StructuredValue.NullValue -> ResolvedFieldValue.NullValue
        is StructuredValue.ObjectValue -> ResolvedFieldValue.StringValue(asDisplayString())
        is StructuredValue.ArrayValue -> ResolvedFieldValue.StringValue(asDisplayString())
    }
}

internal fun parseRawFieldValue(value: String, numericLiteralPattern: Regex): ResolvedFieldValue {
    val normalized = value.trim()
    val scalarValue = when {
        normalized.equals("true", ignoreCase = true) -> ResolvedFieldValue.BooleanValue(true)
        normalized.equals("false", ignoreCase = true) -> ResolvedFieldValue.BooleanValue(false)
        normalized.equals("null", ignoreCase = true) -> ResolvedFieldValue.NullValue
        numericLiteralPattern.matches(normalized) -> {
            runCatching { ResolvedFieldValue.NumberValue(BigDecimal(normalized)) }
                .getOrElse { ResolvedFieldValue.StringValue(value) }
        }

        else -> null
    }
    return scalarValue ?: ResolvedFieldValue.StringValue(value)
}

internal fun QueryLiteral?.toComparableNumber(): BigDecimal? {
    return when (this) {
        is QueryLiteral.NumberValue -> value
        is QueryLiteral.StringValue -> runCatching { BigDecimal(value) }.getOrNull()
        else -> null
    }
}

internal fun QueryLiteral.toSearchToken(): String {
    return when (this) {
        is QueryLiteral.StringValue -> value
        is QueryLiteral.NumberValue -> value.toPlainString()
        is QueryLiteral.BooleanValue -> value.toString()
        QueryLiteral.NullValue -> "null"
    }
}

internal fun ResolvedFieldValue.toComparableNumber(): BigDecimal? {
    return when (this) {
        is ResolvedFieldValue.NumberValue -> value
        is ResolvedFieldValue.StringValue -> runCatching { BigDecimal(value) }.getOrNull()
        else -> null
    }
}

internal fun ResolvedFieldValue.toComparableBoolean(): Boolean? {
    return when (this) {
        is ResolvedFieldValue.BooleanValue -> value
        is ResolvedFieldValue.StringValue -> {
            when {
                value.equals("true", ignoreCase = true) -> true
                value.equals("false", ignoreCase = true) -> false
                else -> null
            }
        }

        else -> null
    }
}

internal fun ResolvedFieldValue.toSearchToken(): String {
    return when (this) {
        is ResolvedFieldValue.StringValue -> value
        is ResolvedFieldValue.NumberValue -> value.toPlainString()
        is ResolvedFieldValue.BooleanValue -> value.toString()
        ResolvedFieldValue.NullValue -> "null"
    }
}
