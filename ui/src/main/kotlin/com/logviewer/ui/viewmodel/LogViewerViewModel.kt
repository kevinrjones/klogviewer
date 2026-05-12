package com.logviewer.ui.viewmodel

import com.logviewer.core.service.LogService
import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogFailure
import com.logviewer.ui.mvi.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LogViewerViewModel(
    private val logService: LogService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LogViewerEvent>()
    val events: SharedFlow<LogViewerEvent> = _events.asSharedFlow()

    fun handleIntent(intent: LogViewerIntent) {
        when (intent) {
            is LogViewerIntent.LoadFile -> loadFile(intent.path)
            LogViewerIntent.ClearLogs -> _state.update { it.copy(logs = emptyList(), filePath = "") }
        }
    }

    private fun loadFile(path: String) {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null, filePath = path) }
            
            // Offload to IO dispatcher
            val result = withContext(Dispatchers.IO) {
                logService.loadLogs(LogFilePath(path))
            }
            
            result.fold(
                ifLeft = { failure ->
                    val message = when (failure) {
                        is LogFailure.FileError -> failure.message
                        is LogFailure.ParsingError -> failure.message
                    }
                    _state.update { it.copy(isLoading = false, error = message) }
                    _events.emit(LogViewerEvent.ShowError(message))
                },
                ifRight = { logs ->
                    _state.update { it.copy(isLoading = false, logs = logs) }
                }
            )
        }
    }
}
