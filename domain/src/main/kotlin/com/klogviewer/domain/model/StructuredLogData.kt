package com.klogviewer.domain.model

data class StructuredLogData(
    val root: StructuredValue.ObjectValue,
    val rawPayload: String? = null,
    val canonicalFields: Map<String, StructuredValue> = emptyMap(),
    val flattenLimits: StructuredFlattenLimits = StructuredFlattenLimits(),
    val cacheLimit: Int = DEFAULT_STRUCTURED_PROJECTION_CACHE_LIMIT,
    val projectionCacheKey: String = rawPayload ?: fallbackProjectionCacheKey(root)
) {
    val flatPathIndex: StructuredPathIndex
        get() = structuredProjectionPathCache.getOrPut(
            cacheLimit = cacheLimit,
            key = "path:$projectionCacheKey"
        ) {
            root.flattenToPathIndex(flattenLimits)
        }

    fun toCompatibilityFields(): Map<String, String> {
        return structuredProjectionCompatibilityCache.getOrPut(
            cacheLimit = cacheLimit,
            key = "compat:$projectionCacheKey"
        ) {
            val flattenedProjection = flatPathIndex
                .toSortedMap()
                .mapValues { (_, values) -> values.toCompatibilityString() }

            val canonicalProjection = canonicalFields
                .toSortedMap()
                .mapValues { (_, value) -> value.asDisplayString() }

            flattenedProjection + canonicalProjection
        }
    }

    companion object {
        const val DEFAULT_STRUCTURED_PROJECTION_CACHE_LIMIT: Int = 2_048
    }
}

internal fun resetStructuredProjectionCachesForTests() {
    structuredProjectionPathCache.clear()
    structuredProjectionCompatibilityCache.clear()
}

internal fun structuredProjectionPathCacheSizeForTests(): Int = structuredProjectionPathCache.size()

internal fun structuredProjectionCompatibilityCacheSizeForTests(): Int = structuredProjectionCompatibilityCache.size()

private val structuredProjectionPathCache = DeterministicLruCache<String, StructuredPathIndex>()

private val structuredProjectionCompatibilityCache = DeterministicLruCache<String, Map<String, String>>()

private class DeterministicLruCache<K, V> {
    private val values = linkedMapOf<K, V>()

    @Synchronized
    fun getOrPut(cacheLimit: Int, key: K, supplier: () -> V): V {
        values.remove(key)?.let { cachedValue ->
            values[key] = cachedValue
            return cachedValue
        }

        val createdValue = supplier()
        values[key] = createdValue
        trim(cacheLimit.coerceAtLeast(1))
        return createdValue
    }

    @Synchronized
    fun clear() {
        values.clear()
    }

    @Synchronized
    fun size(): Int = values.size

    private fun trim(limit: Int) {
        while (values.size > limit) {
            val oldestKey = values.entries.first().key
            values.remove(oldestKey)
        }
    }
}

private fun fallbackProjectionCacheKey(root: StructuredValue.ObjectValue): String {
    return "root-${System.identityHashCode(root)}"
}

private fun List<StructuredValue>.toCompatibilityString(): String {
    return if (size == 1) {
        first().asDisplayString()
    } else {
        joinToString(separator = ",") { value -> value.asDisplayString() }
    }
}
