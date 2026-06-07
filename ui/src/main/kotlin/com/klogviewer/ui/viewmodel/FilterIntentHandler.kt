package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.TimeRangePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

class FilterIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val onSavePreferences: () -> Unit,
    private val onFilterLogs: (String?) -> Unit
) {
    fun handle(intent: KLogViewerIntent.FilterIntent) {
        when (intent) {
            is KLogViewerIntent.AddFilterQuery -> {
                if (intent.query.isNotBlank()) {
                    state.update { currentState ->
                        currentState.updateActiveWindow { window ->
                            if (!window.filterQueries.contains(intent.query)) {
                                window.copy(filterQueries = window.filterQueries + intent.query)
                            } else window
                        }
                    }
                    onFilterLogs(state.value.activeTab?.activeWindow?.id)
                    onSavePreferences()
                }
            }
            is KLogViewerIntent.RemoveFilterQuery -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = window.filterQueries - intent.query)
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ClearFilterQueries -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = emptyList())
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            is KLogViewerIntent.ToggleLevel -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val newFilters = LevelFilterPolicy.toggle(window.levelFilters, intent.level)
                        window.copy(levelFilters = newFilters)
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            is KLogViewerIntent.SetTimeFilterFrom -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val fromValue = intent.from.trim()
                        val fromInstant = TimeRangeFilterSupport.parseInstantOrNull(fromValue)
                        val validationMessage = TimeRangeFilterSupport.validationMessage(
                            fromValue,
                            fromInstant,
                            window.timeFilterTo,
                            window.timeFilterToInstant
                        )
                        window.copy(
                            timeFilterFrom = fromValue,
                            timeFilterFromInstant = fromInstant,
                            timeFilterPreset = manualPresetOrNull(fromValue, window.timeFilterTo),
                            timeFilterValidationMessage = validationMessage
                        )
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            is KLogViewerIntent.SetTimeFilterTo -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val toValue = intent.to.trim()
                        val toInstant = TimeRangeFilterSupport.parseInstantOrNull(toValue)
                        val validationMessage = TimeRangeFilterSupport.validationMessage(
                            window.timeFilterFrom,
                            window.timeFilterFromInstant,
                            toValue,
                            toInstant
                        )
                        window.copy(
                            timeFilterTo = toValue,
                            timeFilterToInstant = toInstant,
                            timeFilterPreset = manualPresetOrNull(window.timeFilterFrom, toValue),
                            timeFilterValidationMessage = validationMessage
                        )
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            is KLogViewerIntent.ApplyTimeFilterPreset -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val resolvedRange = TimeRangeFilterSupport.resolvePresetSelection(window, intent.preset)
                        val fromInstant = resolvedRange?.first
                        val toInstant = resolvedRange?.second
                        val fromValue = fromInstant?.toString().orEmpty()
                        val toValue = toInstant?.toString().orEmpty()
                        val validationMessage = TimeRangeFilterSupport.validationMessage(
                            fromValue,
                            fromInstant,
                            toValue,
                            toInstant
                        )

                        window.copy(
                            timeFilterFrom = fromValue,
                            timeFilterTo = toValue,
                            timeFilterFromInstant = fromInstant,
                            timeFilterToInstant = toInstant,
                            timeFilterPreset = intent.preset,
                            timeFilterValidationMessage = validationMessage
                        )
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ClearTimeFilter -> {
                val clearedAt = Instant.now()
                val fromValue = clearedAt.toString()
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val validationMessage = TimeRangeFilterSupport.validationMessage(
                            fromValue,
                            clearedAt,
                            "",
                            null
                        )

                        window.copy(
                            timeFilterFrom = fromValue,
                            timeFilterTo = "",
                            timeFilterFromInstant = clearedAt,
                            timeFilterToInstant = null,
                            timeFilterPreset = TimeRangePreset.CUSTOM,
                            timeFilterValidationMessage = validationMessage
                        )
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ResetTimeFilter -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(
                            timeFilterFrom = "",
                            timeFilterTo = "",
                            timeFilterFromInstant = null,
                            timeFilterToInstant = null,
                            timeFilterPreset = null,
                            timeFilterValidationMessage = null
                        )
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ToggleAllLevels -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val newFilters = LevelFilterPolicy.toggleAll(
                            filters = window.levelFilters,
                            availableLevels = window.availableLevels.toSet()
                        )
                        window.copy(levelFilters = newFilters)
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
        }
    }

    private fun manualPresetOrNull(fromValue: String, toValue: String): TimeRangePreset? {
        return if (fromValue.isNotBlank() || toValue.isNotBlank()) {
            TimeRangePreset.CUSTOM
        } else {
            null
        }
    }

}
