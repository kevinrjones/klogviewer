package com.klogviewer.ui.components

private enum class StructuredActionTag(val prefix: String) {
    COPY_PATH("structured_action_copy_path"),
    COPY_VALUE("structured_action_copy_value"),
    FILTER_FIELD("structured_action_filter_field"),
    FILTER_VALUE("structured_action_filter_value")
}

fun copyPathActionTag(path: String): String {
    return actionTag(StructuredActionTag.COPY_PATH, path)
}

fun copyValueActionTag(path: String): String {
    return actionTag(StructuredActionTag.COPY_VALUE, path)
}

fun filterFieldActionTag(path: String): String {
    return actionTag(StructuredActionTag.FILTER_FIELD, path)
}

fun filterValueActionTag(path: String): String {
    return actionTag(StructuredActionTag.FILTER_VALUE, path)
}

private fun actionTag(action: StructuredActionTag, path: String): String {
    return "${action.prefix}_${path.toTagSegment()}"
}

private fun String.toTagSegment(): String {
    return if (isBlank()) {
        "root"
    } else {
        map { char ->
            if (char.isLetterOrDigit()) {
                char
            } else {
                '_'
            }
        }.joinToString(separator = "")
    }
}
