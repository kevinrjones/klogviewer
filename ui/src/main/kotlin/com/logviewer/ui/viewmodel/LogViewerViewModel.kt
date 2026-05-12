package com.logviewer.ui.viewmodel

import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogFailure
import com.logviewer.domain.model.LogUpdate
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel
import com.logviewer.domain.repository.LogSource
import com.logviewer.ui.mvi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogViewerViewModel(
    private val logSource: LogSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LogViewerEvent>()
    val events: SharedFlow<LogViewerEvent> = _events.asSharedFlow()

    private var logJob: Job? = null

    init {
        _state
            .map { Triple(it.logs, it.searchQuery, it.levelFilters) }
            .distinctUntilChanged()
            .onEach { (logs, query, levels) ->
                filterLogs(logs, query, levels)
            }
            .launchIn(scope)
    }

    private fun filterLogs(logs: List<LogEntry>, query: String, levels: Set<LogLevel>) {
        scope.launch(Dispatchers.Default) {
            val filtered = logs.filter { entry ->
                val matchesLevel = levels.contains(entry.level)
                val matchesSearch = if (query.isEmpty()) {
                    true
                } else {
                    entry.content.value.contains(query, ignoreCase = true) ||
                    entry.timestamp.value.contains(query, ignoreCase = true)
                }
                matchesLevel && matchesSearch
            }
            _state.update { it.copy(filteredLogs = filtered) }
        }
    }

    fun handleIntent(intent: LogViewerIntent) {
        when (intent) {
            is LogViewerIntent.LoadFile -> loadFile(intent.path)
            is LogViewerIntent.SelectPath -> _state.update { it.copy(filePath = intent.path) }
            LogViewerIntent.ClearLogs -> {
                logJob?.cancel()
                _state.update { it.copy(logs = emptyList(), filePath = "") }
            }
            LogViewerIntent.ToggleTheme -> {
                _state.update { it.copy(isDarkMode = !it.isDarkMode) }
            }
            LogViewerIntent.ToggleSidebar -> {
                _state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
            }
            is LogViewerIntent.UpdateSearch -> {
                _state.update { it.copy(searchQuery = intent.query) }
            }
            is LogViewerIntent.ToggleLevel -> {
                _state.update { currentState ->
                    val newFilters = if (currentState.levelFilters.contains(intent.level)) {
                        currentState.levelFilters - intent.level
                    } else {
                        currentState.levelFilters + intent.level
                    }
                    currentState.copy(levelFilters = newFilters)
                }
            }
        }
    }

    private fun loadFile(path: String) {
        logJob?.cancel()
        logJob = scope.launch {
            _state.update { it.copy(isLoading = true, error = null, filePath = path, logs = emptyList()) }
            
            logSource.observeLogs(LogFilePath(path))
                .collect { result ->
                    result.fold(
                        ifLeft = { failure ->
                            val message = when (failure) {
                                is LogFailure.FileError -> failure.message
                                is LogFailure.ParsingError -> failure.message
                            }
                            _state.update { it.copy(isLoading = false, error = message) }
                            _events.emit(LogViewerEvent.ShowError(message))
                        },
                        ifRight = { update ->
                            handleLogUpdate(update)
                        }
                    )
                }
        }
    }

    private fun handleLogUpdate(update: LogUpdate) {
        when (update) {
            is LogUpdate.Initial -> _state.update { it.copy(isLoading = false, logs = update.entries) }
            is LogUpdate.Appended -> _state.update { it.copy(logs = it.logs + update.entries) }
            LogUpdate.Reset -> _state.update { it.copy(logs = emptyList()) }
        }
    }
}
