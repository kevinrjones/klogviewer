package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

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
                        val newFilters = if (window.levelFilters.contains(intent.level)) {
                            window.levelFilters - intent.level
                        } else {
                            window.levelFilters + intent.level
                        }
                        window.copy(levelFilters = newFilters)
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
            KLogViewerIntent.ToggleAllLevels -> {
                state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val allLevels = LogLevel.entries.toSet()
                        val newFilters = if (window.levelFilters.size == allLevels.size) {
                            emptySet()
                        } else {
                            allLevels
                        }
                        window.copy(levelFilters = newFilters)
                    }
                }
                onFilterLogs(state.value.activeTab?.activeWindow?.id)
                onSavePreferences()
            }
        }
    }
}
