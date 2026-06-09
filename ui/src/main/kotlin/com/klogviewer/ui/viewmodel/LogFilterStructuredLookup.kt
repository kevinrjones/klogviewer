package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.StructuredValue

private val STRUCTURED_PATH_DELIMITERS = listOf('.', '[', ']', '`')

internal fun resolveStructuredValuesForCandidate(
    structuredPathIndex: Map<String, List<StructuredValue>>,
    candidatePath: String
): List<StructuredValue> {
    val directMatch = structuredPathIndex[candidatePath]
    val caseVariantMatches = if (allowsCaseVariantFallback(candidatePath)) {
        structuredPathIndex.entries
            .filter { (path, _) -> path.equals(candidatePath, ignoreCase = true) }
            .flatMap { (_, values) -> values }
    } else {
        emptyList()
    }
    return directMatch ?: caseVariantMatches
}

internal fun resolveCompatibilityFieldValue(
    compatibilityFields: Map<String, String>,
    candidatePath: String
): String? {
    val directMatch = compatibilityFields[candidatePath]
    val caseVariantMatch = if (allowsCaseVariantFallback(candidatePath)) {
        compatibilityFields.entries
            .firstOrNull { (path, _) -> path.equals(candidatePath, ignoreCase = true) }
            ?.value
    } else {
        null
    }
    return directMatch ?: caseVariantMatch
}

internal fun containsResolvedPath(
    structuredPathIndex: Map<String, List<StructuredValue>>,
    compatibilityFields: Map<String, String>,
    candidatePath: String
): Boolean {
    val hasDirectMatch =
        structuredPathIndex.containsKey(candidatePath) || compatibilityFields.containsKey(candidatePath)
    val hasCaseVariantMatch = allowsCaseVariantFallback(candidatePath) && (
        structuredPathIndex.keys.any { path -> path.equals(candidatePath, ignoreCase = true) } ||
            compatibilityFields.keys.any { path -> path.equals(candidatePath, ignoreCase = true) }
        )
    return hasDirectMatch || hasCaseVariantMatch
}

private fun allowsCaseVariantFallback(candidatePath: String): Boolean {
    val pathWithoutAtPrefix = candidatePath.removePrefix("@")
    val hasStructuralDelimiter = STRUCTURED_PATH_DELIMITERS.any { delimiter ->
        pathWithoutAtPrefix.contains(delimiter)
    }
    return when {
        candidatePath.contains('_') -> true
        pathWithoutAtPrefix.isEmpty() -> false
        hasStructuralDelimiter -> false
        else -> pathWithoutAtPrefix.drop(1).none { character -> character.isUpperCase() }
    }
}
