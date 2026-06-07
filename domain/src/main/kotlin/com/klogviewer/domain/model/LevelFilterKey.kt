package com.klogviewer.domain.model

import kotlin.jvm.JvmInline

/**
 * Represents a unique key for filtering log levels. This class is implemented as a value class
 * to encapsulate a `String` value that corresponds to a log level.
 */
@JvmInline
value class LevelFilterKey private constructor(val value: String) {
    /**
     * Companion object providing utility functions and default configurations for the `LevelFilterKey` class.
     */
    companion object {
        /**
         * A set of default `LevelFilterKey` values derived from all entries of the `LogLevel` enumeration.
         *
         * This set is constructed by mapping each log level from `LogLevel.entries` to a `LevelFilterKey`
         * using the `fromLogLevel` function and collecting the results into a set.
         *
         * The defaults include all possible log levels defined in `LogLevel` and are useful
         * for initializing configurations or applying filters across all recognized log levels.
         */
        val defaults: Set<LevelFilterKey> = LogLevel.entries.map { fromLogLevel(it) }.toSet()

        /**
         * Converts a given LogLevel to a LevelFilterKey.
         *
         * @param level The LogLevel to be converted.
         * @return The corresponding LevelFilterKey derived from the LogLevel.
         */
        fun fromLogLevel(level: LogLevel): LevelFilterKey = LevelFilterKey(level.name)

        /**
         * Attempts to create a LevelFilterKey from a raw string value.
         * The input is trimmed, converted to uppercase, and validated to ensure it is not empty.
         * If the input is null, blank, or invalid, the function returns null.
         *
         * @param rawValue The raw string value to be processed into a LevelFilterKey.
         * @return A LevelFilterKey instance if the raw string is valid, or null if it is null, blank, or invalid.
         */
        fun fromRawOrNull(rawValue: String?): LevelFilterKey? {
            val normalized = rawValue
                ?.trim()
                ?.uppercase()
                ?.takeIf { it.isNotEmpty() }
                ?: return null
            return LevelFilterKey(normalized)
        }

        /**
         * Converts a collection of raw string values into a set of {@code LevelFilterKey} instances.
         *
         * @param rawValues an iterable collection of raw string values to be converted. Each value is
         *                  normalized and transformed into a {@code LevelFilterKey} if valid.
         * @return a set of {@code LevelFilterKey} instances created from the valid raw values. If no valid
         *         values are provided, an empty set is returned.
         */
        fun fromRawValues(rawValues: Iterable<String>): Set<LevelFilterKey> {
            return rawValues.mapNotNull { fromRawOrNull(it) }.toSet()
        }
    }
}