package com.klogviewer.ui.components

import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.domain.model.asDisplayString

fun isContainerNode(value: StructuredValue): Boolean {
    return value is StructuredValue.ObjectValue || value is StructuredValue.ArrayValue
}

fun structuredTypeLabel(value: StructuredValue): String {
    return when (value) {
        is StructuredValue.StringValue -> "string"
        is StructuredValue.NumberValue -> "number"
        is StructuredValue.BooleanValue -> "bool"
        StructuredValue.NullValue -> "null"
        is StructuredValue.ObjectValue -> "object"
        is StructuredValue.ArrayValue -> "array"
    }
}

fun scalarDisplayValue(value: StructuredValue): String {
    return when (value) {
        is StructuredValue.StringValue -> "\"${value.value}\""
        is StructuredValue.NumberValue -> value.value
        is StructuredValue.BooleanValue -> value.value.toString()
        StructuredValue.NullValue -> "null"
        else -> value.asDisplayString()
    }
}

fun copyValueText(value: StructuredValue): String {
    return when (value) {
        is StructuredValue.StringValue -> value.value
        is StructuredValue.NumberValue -> value.value
        is StructuredValue.BooleanValue -> value.value.toString()
        StructuredValue.NullValue -> "null"
        else -> value.asDisplayString()
    }
}
