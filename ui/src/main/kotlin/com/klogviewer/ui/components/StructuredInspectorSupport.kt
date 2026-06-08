package com.klogviewer.ui.components

import com.klogviewer.domain.model.StructuredValue

const val MAX_STRUCTURED_CHILDREN_PER_NODE: Int = 200
const val MAX_STRUCTURED_SCALAR_PREVIEW_LENGTH: Int = 512

data class StructuredInspectorActions(
    val onCopyPath: (String) -> Unit = {},
    val onCopyValue: (String) -> Unit = {},
    val onFilterByField: (String) -> Unit = {},
    val onFilterByValue: (String, StructuredValue) -> Unit = { _, _ -> }
)

data class StructuredChild(
    val label: String,
    val path: String,
    val value: StructuredValue
)
