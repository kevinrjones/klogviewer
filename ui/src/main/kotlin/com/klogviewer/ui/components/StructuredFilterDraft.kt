package com.klogviewer.ui.components

internal data class StructuredFilterOperator(
    val id: String,
    val token: String,
    val label: String,
    val requiresValue: Boolean = true
)

internal data class StructuredFilterDraftState(
    val fieldPath: String = "",
    val operatorId: String = STRUCTURED_FILTER_OPERATORS.first().id,
    val value: String = ""
)

internal val STRUCTURED_FILTER_OPERATORS = listOf(
    StructuredFilterOperator(id = "eq", token = "=", label = "Equals"),
    StructuredFilterOperator(id = "contains", token = "contains", label = "Contains"),
    StructuredFilterOperator(id = "regex", token = "~", label = "Regex"),
    StructuredFilterOperator(id = "gt", token = ">", label = "Greater than"),
    StructuredFilterOperator(id = "gte", token = ">=", label = "Greater than or equal"),
    StructuredFilterOperator(id = "lt", token = "<", label = "Less than"),
    StructuredFilterOperator(id = "lte", token = "<=", label = "Less than or equal"),
    StructuredFilterOperator(id = "exists", token = "exists", label = "Exists", requiresValue = false),
    StructuredFilterOperator(id = "missing", token = "missing", label = "Missing", requiresValue = false)
)

private val numericValuePattern = Regex("^-?\\d+(\\.\\d+)?$")

internal fun StructuredFilterDraftState.selectedOperator(): StructuredFilterOperator {
    return STRUCTURED_FILTER_OPERATORS.firstOrNull { operator ->
        operator.id == operatorId
    } ?: STRUCTURED_FILTER_OPERATORS.first()
}

internal fun StructuredFilterDraftState.canApply(): Boolean {
    val selectedOperator = selectedOperator()
    return fieldPath.trim().isNotEmpty() &&
        (!selectedOperator.requiresValue || value.trim().isNotEmpty())
}

internal fun StructuredFilterDraftState.buildQuery(): String {
    return buildStructuredFilterQuery(
        path = fieldPath,
        operator = selectedOperator(),
        rawValue = value
    )
}

private fun buildStructuredFilterQuery(
    path: String,
    operator: StructuredFilterOperator,
    rawValue: String
): String {
    val normalizedPath = path.trim()
    if (!operator.requiresValue) {
        return "field:$normalizedPath ${operator.token}"
    }

    val valueToken = toStructuredFilterValueToken(rawValue.trim())
    return if (operator.token == "=") {
        "field:$normalizedPath=${valueToken}"
    } else {
        "field:$normalizedPath ${operator.token} $valueToken"
    }
}

private fun toStructuredFilterValueToken(value: String): String {
    val lowercaseValue = value.lowercase()
    val normalizedScalar = when {
        lowercaseValue == "true" -> lowercaseValue
        lowercaseValue == "false" -> lowercaseValue
        lowercaseValue == "null" -> lowercaseValue
        numericValuePattern.matches(value) -> value
        else -> null
    }
    return normalizedScalar ?: "\"${escapeStructuredStringValue(value)}\""
}

private fun escapeStructuredStringValue(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}
