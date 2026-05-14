package com.logviewer.ui.viewmodel

import com.logviewer.domain.model.*
import com.logviewer.domain.repository.LogSource
import com.logviewer.core.source.MergedLogSource
import com.logviewer.ui.mvi.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

class LogViewerViewModel(
    private val logSource: LogSource,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LogViewerEvent>()
    val events: SharedFlow<LogViewerEvent> = _events.asSharedFlow()

    private val logJobs = mutableMapOf<String, Job>()

    fun handleIntent(intent: LogViewerIntent) {
        logger.debug { "Handling intent: ${intent::class.simpleName}" }
        when (intent) {
            is LogViewerIntent.LoadFiles -> loadFiles(intent.paths)
            is LogViewerIntent.AddToWorkspace -> {
                val currentPaths = _state.value.activeTab?.sourceIds ?: emptyList()
                loadFiles(currentPaths + intent.paths)
            }
            is LogViewerIntent.SelectPath -> _state.update { it.updateActiveTab { tab -> tab.copy(filePath = intent.path) } }
            LogViewerIntent.ClearLogs -> clearActiveTab()
            LogViewerIntent.ToggleTheme -> _state.update { it.copy(isDarkMode = !it.isDarkMode) }
            LogViewerIntent.ToggleSidebar -> _state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
            is LogViewerIntent.UpdateSearch -> {
                _state.update { it.updateActiveTab { tab -> tab.copy(searchQuery = intent.query) } }
                filterLogs(_state.value.activeTabId)
            }
            is LogViewerIntent.ToggleLevel -> {
                _state.update { currentState ->
                    currentState.updateActiveTab { tab ->
                        val newFilters = if (tab.levelFilters.contains(intent.level)) {
                            tab.levelFilters - intent.level
                        } else {
                            tab.levelFilters + intent.level
                        }
                        tab.copy(levelFilters = newFilters)
                    }
                }
                filterLogs(_state.value.activeTabId)
            }
            LogViewerIntent.AddTab -> addTab()
            is LogViewerIntent.CloseTab -> closeTab(intent.id)
            is LogViewerIntent.SwitchTab -> {
                _state.update { it.copy(activeTabId = intent.id) }
            }
            is LogViewerIntent.SelectEntry -> {
                _state.update { it.updateActiveTab { tab -> tab.copy(selectedEntry = intent.entry) } }
            }
            LogViewerIntent.ShowOpenDialog -> _state.update { it.copy(pendingDialog = LogViewerState.DialogType.OPEN) }
            LogViewerIntent.ShowAddDialog -> _state.update { it.copy(pendingDialog = LogViewerState.DialogType.ADD) }
            LogViewerIntent.DismissDialog -> _state.update { it.copy(pendingDialog = null) }
        }
    }

    private fun addTab() {
        val newId = UUID.randomUUID().toString()
        val newTab = TabState(id = newId, title = "New Tab")
        _state.update { it.copy(tabs = it.tabs + newTab, activeTabId = newId) }
    }

    private fun closeTab(id: String) {
        logJobs[id]?.cancel()
        logJobs.remove(id)
        _state.update { currentState ->
            val remainingTabs = currentState.tabs.filter { it.id != id }
            val newTabs = if (remainingTabs.isEmpty()) {
                listOf(TabState(id = UUID.randomUUID().toString(), title = "Log View"))
            } else {
                remainingTabs
            }
            val newActiveId = if (currentState.activeTabId == id) {
                newTabs.last().id
            } else {
                currentState.activeTabId
            }
            currentState.copy(tabs = newTabs, activeTabId = newActiveId)
        }
    }

    private fun clearActiveTab() {
        val activeTabId = _state.value.activeTabId ?: return
        logJobs[activeTabId]?.cancel()
        _state.update { it.updateActiveTab { tab -> tab.copy(logs = emptyList(), filePath = "", sourceIds = emptyList(), title = "Log View") } }
        filterLogs(activeTabId)
    }

    private fun loadFiles(paths: List<String>) {
        val activeTabId = _state.value.activeTabId ?: return
        
        logJobs[activeTabId]?.cancel()
        logJobs[activeTabId] = scope.launch {
            val title = if (paths.size == 1) File(paths[0]).name else "${paths.size} files"
            _state.update { currentState ->
                currentState.updateActiveTab { tab -> 
                    tab.copy(
                        isLoading = true, 
                        error = null, 
                        filePath = paths.joinToString(", "), 
                        logs = emptyList(), 
                        sourceIds = paths,
                        title = title
                    ) 
                }
            }
            
            val flow = if (paths.size == 1) {
                logSource.observeLogs(LogFilePath(paths[0]))
            } else {
                val sources = paths.map { logSource to LogFilePath(it) }
                MergedLogSource(sources).observeMerged()
            }
            
            flow.collect { result ->
                result.fold(
                    ifLeft = { failure ->
                        val message = when (failure) {
                            is LogFailure.FileError -> failure.message
                            is LogFailure.ParsingError -> failure.message
                        }
                        logger.error { "Failed to load logs: $message" }
                        _state.update { it.updateActiveTab { tab -> tab.copy(isLoading = false, error = message) } }
                        _events.emit(LogViewerEvent.ShowError(message))
                    },
                    ifRight = { update ->
                        handleLogUpdate(activeTabId, update)
                    }
                )
            }
        }
    }

    private fun handleLogUpdate(tabId: String, update: LogUpdate) {
        _state.update { currentState ->
            currentState.copy(tabs = currentState.tabs.map { tab ->
                if (tab.id == tabId) {
                    when (update) {
                        is LogUpdate.Initial -> tab.copy(isLoading = false, logs = update.entries)
                        is LogUpdate.Appended -> tab.copy(logs = tab.logs + update.entries)
                        LogUpdate.Reset -> tab.copy(logs = emptyList())
                    }
                } else tab
            })
        }
        filterLogs(tabId)
    }

    private fun filterLogs(tabId: String?) {
        if (tabId == null) return
        
        scope.launch(Dispatchers.Default) {
            val tab = _state.value.tabs.find { it.id == tabId } ?: return@launch
            
            val filtered = tab.logs.filter { entry ->
                val matchesLevel = tab.levelFilters.contains(entry.level)
                val matchesSearch = if (tab.searchQuery.isEmpty()) {
                    true
                } else {
                    entry.content.value.contains(tab.searchQuery, ignoreCase = true) ||
                    entry.timestamp.value.contains(tab.searchQuery, ignoreCase = true)
                }
                matchesLevel && matchesSearch
            }
            
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { 
                    if (it.id == tabId) it.copy(filteredLogs = filtered) else it 
                })
            }
        }
    }
}
