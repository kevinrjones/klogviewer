package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.CanonicalFieldAliases

internal interface QueryPathResolver {
    fun candidatePaths(path: String, isExplicitFieldPath: Boolean): List<String>
}

internal class CanonicalQueryPathResolver(
    private val canonicalAliasPaths: Map<String, List<String>> = DEFAULT_CANONICAL_ALIAS_PATHS
) : QueryPathResolver {
    override fun candidatePaths(path: String, isExplicitFieldPath: Boolean): List<String> {
        val aliasBasePaths = if (isExplicitFieldPath) {
            listOf(path)
        } else {
            canonicalAliasPaths[path.lowercase()]
                ?.let { aliases -> (listOf(path) + aliases).distinct() }
                ?: listOf(path)
        }

        return aliasBasePaths
            .flatMap(::expandLookupCandidates)
            .distinct()
    }

    private fun expandLookupCandidates(candidatePath: String): List<String> {
        return StructuredQueryPath.parse(candidatePath)
            ?.toLookupCandidates()
            ?: listOf(candidatePath)
    }

    private companion object {
        private val DEFAULT_CANONICAL_ALIAS_PATHS = CanonicalFieldAliases.QUERY_CANONICAL_ALIAS_PATHS
    }
}
