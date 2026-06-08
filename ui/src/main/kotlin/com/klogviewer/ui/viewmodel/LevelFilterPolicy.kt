package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LevelFilterKey
import com.klogviewer.domain.model.LogEntry

/**
 * Provides utility methods and state for managing level-based log filtering.
 */
internal object LevelFilterPolicy {
    /**
     * A predefined set of default log level filters.
     *
     * This variable represents the default configuration for filtering log levels,
     * leveraging all entries defined in the `LogLevel` enum. It provides an initial
     * state for systems or tools that depend on log level filtering, ensuring that
     * all available log levels are included by default.
     */
    val defaultFilters: Set<LevelFilterKey> = LevelFilterKey.defaults

    /**
     * Determines the list of distinct log levels present in the given log entries
     * and sorts them using a predefined ranking order.
     *
     * @param logs A list of log entries from which available levels are to be extracted.
     * @return A sorted list of unique level filter keys representing the available levels.
     */
    fun availableLevels(logs: List<LogEntry>): List<LevelFilterKey> {
        val levelCounts = logs.groupingBy { resolveLevelKey(it) }.eachCount()
        return levelCounts.keys.sortedWith(compareBy({ levelSortRank(it) }, { it.value }))
    }

    /**
     * Toggles the presence of a `LevelFilterKey` in the set of filters. If the specified level is
     * present in the set, it will be removed. Otherwise, it will be added.
     *
     * @param filters The set of `LevelFilterKey` objects representing current filters.
     * @param level The `LevelFilterKey` to toggle in the set.
     * @return A new set of `LevelFilterKey` objects with the specified level toggled.
     */
    fun toggle(filters: Set<LevelFilterKey>, level: LevelFilterKey): Set<LevelFilterKey> {
        return if (filters.contains(level)) {
            filters - level
        } else {
            filters + level
        }
    }

    /**
     * Toggles the selected filters to either enable all available levels or disable all filters
     * based on the current selection.
     *
     * @param filters A set of currently selected filters represented by LevelFilterKey.
     * @param availableLevels A set of available filter levels represented by LevelFilterKey.
     * @return A set of LevelFilterKey that contains either all available levels if not all are selected,
     *         or an empty set if all levels were selected, effectively toggling the selection state.
     */
    fun toggleAll(
        filters: Set<LevelFilterKey>,
        availableLevels: Set<LevelFilterKey>
    ): Set<LevelFilterKey> {
        val allLevels = availableLevels.ifEmpty { defaultFilters }
        val hasAllLevelsEnabled = allLevels.isNotEmpty() && filters.containsAll(allLevels)
        return if (hasAllLevelsEnabled) {
            emptySet()
        } else {
            allLevels
        }
    }

    /**
     * Reconciles the current set of level filters based on the provided previous state and updates.
     *
     * @param previousFilters A set of level filters that were previously selected.
     * @param previousAvailableLevels A set of levels that were previously available in the system.
     * @param updatedAvailableLevels A set of levels that are currently available in the system.
     * @return A set of level filters after reconciling the changes,
     * ensuring compatibility with the updated available levels.
     */
    fun reconcile(
        previousFilters: Set<LevelFilterKey>,
        previousAvailableLevels: Set<LevelFilterKey>,
        updatedAvailableLevels: Set<LevelFilterKey>
    ): Set<LevelFilterKey> {
        val hadAllPreviouslyEnabled = previousFilters.containsAll(previousAvailableLevels)
        return when {
            updatedAvailableLevels.isEmpty() -> previousFilters
            previousFilters.isEmpty() -> emptySet()
            hadAllPreviouslyEnabled -> updatedAvailableLevels
            else -> previousFilters.intersect(updatedAvailableLevels)
        }
    }

    /**
     * Checks if the given log entry matches any of the specified level filters.
     *
     * @param entry the log entry to be evaluated against the filters.
     * @param filters the set of level filter keys to match the log entry against.
     * @return `true` if the log entry matches one of the filters, otherwise `false`.
     */
    fun matches(entry: LogEntry, filters: Set<LevelFilterKey>): Boolean {
        return filters.contains(resolveLevelKey(entry))
    }

    /**
     * Converts a set of raw filter strings into a set of typed `LevelFilterKey` objects.
     *
     * @param rawFilters A set of string representations of filter values to be converted.
     * @return A set of `LevelFilterKey` objects derived from the provided raw filter strings.
     */
    fun toTypedFilters(rawFilters: Set<String>): Set<LevelFilterKey> = LevelFilterKey.fromRawValues(rawFilters)

    /**
     * Converts a set of LevelFilterKey objects into a set of their corresponding raw string values.
     *
     * @param filters A set of LevelFilterKey instances to be converted.
     * @return A set of string values corresponding to the `value` property of each LevelFilterKey in the input set.
     */
    fun toRawFilters(filters: Set<LevelFilterKey>): Set<String> = filters.map { it.value }.toSet()

    /**
     * Resolves the log level key for the given log entry.
     *
     * @param entry The log entry from which the level key will be determined.
     * @return The resolved LevelFilterKey, derived either from the raw level key field in the log entry
     * or the log entry's level.
     */
    fun resolveLevelKey(entry: LogEntry): LevelFilterKey {
        return LevelFilterKey.fromRawOrNull(entry.resolvedLevelKey())
            ?: LevelFilterKey.fromLogLevel(entry.level)
    }

    /**
     * Calculates a numeric rank for a given logging level based on its severity.
     *
     * @param level The logging level wrapped in a `LevelFilterKey` instance, representing a specific
     * severity level such as TRACE, DEBUG, INFO, WARN (or WARNING), ERROR, FATAL, or UNKNOWN.
     * @return An integer representing the rank of the provided logging level. Lower numbers
     * indicate lower severity (e.g., TRACE), while higher numbers indicate higher
     * severity (e.g., FATAL). Levels not explicitly mapped return a default value.
     */
    private fun levelSortRank(level: LevelFilterKey): Int {
        return when (level.value) {
            "TRACE" -> LEVEL_SORT_TRACE
            "DEBUG" -> LEVEL_SORT_DEBUG
            "INFO" -> LEVEL_SORT_INFO
            "WARN", "WARNING" -> LEVEL_SORT_WARN
            "ERROR" -> LEVEL_SORT_ERROR
            "FATAL" -> LEVEL_SORT_FATAL
            "UNKNOWN" -> LEVEL_SORT_UNKNOWN
            else -> LEVEL_SORT_OTHER
        }
    }

    /**
     * A constant representing the trace level for sorting operations in the log viewer.
     * Used as a debugging aid to enable or disable detailed tracing during log level sorting.
     * The value `0` typically indicates that tracing is disabled.
     */
    private const val LEVEL_SORT_TRACE = 0
    /**
     * A constant flag used for debugging purposes during the level sorting process.
     * Its value can be used to enable or disable additional debug behavior or logging
     * in areas of the application where level sorting is implemented or evaluated.
     *
     * Developers can utilize this constant to trace issues or verify the correctness
     * of the sorting logic in specific debugging scenarios.
     */
    private const val LEVEL_SORT_DEBUG = 1
    /**
     * A constant representing the sorting index or priority level for log entries.
     * This value is used to determine the order in which log entries are processed or displayed,
     * based on their priority or importance.
     */
    private const val LEVEL_SORT_INFO = 2
    /**
     * Constant representing the log level threshold used to trigger warning messages during
     * sorting operations. Sorting operations that meet or exceed this level may result in a
     * warning being logged or displayed to the user.
     *
     * Typically used in log filtering, sorting, or visualization contexts where specific
     * log levels influence behavior or visualization thresholds.
     */
    private const val LEVEL_SORT_WARN = 3
    /**
     * Represents the error level code associated with issues during the sorting of log levels.
     * This constant is used to identify and categorize sorting-related errors within the system.
     */
    private const val LEVEL_SORT_ERROR = 4
    /**
     * Represents the severity level used for sorting or filtering log entries, where `5` indicates the
     * highest severity level, typically used for fatal errors.
     *
     * The `LEVEL_SORT_FATAL` constant is intended to be used as a reference value for comparing or
     * categorizing log levels in the system. Log levels with higher values are considered more severe.
     *
     * Usage of this constant helps enforce consistency when handling log severity levels across the
     * application.
     */
    private const val LEVEL_SORT_FATAL = 5
    /**
     * Specifies the numeric value corresponding to an unknown log level sorting order.
     *
     * This constant is used internally to represent a state where the log level sorting
     * is not determined or is unspecified. It serves as a fallback or default value
     * in log filtering or sorting operations.
     */
    private const val LEVEL_SORT_UNKNOWN = 6
    /**
     * A constant used for sorting log levels that do not fall under predefined categories.
     * This value represents the sort order for "other" levels when organizing or filtering log entries.
     */
    private const val LEVEL_SORT_OTHER = 7
}
