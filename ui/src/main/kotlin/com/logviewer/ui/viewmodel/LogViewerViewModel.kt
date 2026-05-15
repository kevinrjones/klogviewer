package com.logviewer.ui.viewmodel

import com.logviewer.domain.model.*
import com.logviewer.domain.repository.LogSource
import com.logviewer.domain.parser.LogParser
import com.logviewer.core.parser.HeuristicProbe
import com.logviewer.core.source.MergedLogSource
import com.logviewer.core.repository.PreferencesRepository
import com.logviewer.ui.mvi.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID

private val logger = KotlinLogging.logger {}

class LogViewerViewModel(
    private val logSource: LogSource,
    private val prefsRepository: PreferencesRepository,
    private val heuristicProbe: HeuristicProbe,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(LogViewerState())
    val state: StateFlow<LogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LogViewerEvent>()
    val events: SharedFlow<LogViewerEvent> = _events.asSharedFlow()

    private val logJobs = mutableMapOf<String, Job>()
    private var saveJob: Job? = null

    init {
        val prefs = prefsRepository.load()
        if (prefs.tabs.isNotEmpty()) {
            val initialState = LogViewerState(
                tabs = prefs.tabs.map { tp ->
                    TabState(
                        id = tp.id,
                        title = tp.title,
                        windows = tp.windows.map { wp ->
                            LogWindow(
                                id = wp.id,
                                filePath = wp.filePath,
                                sourceIds = wp.sourceIds,
                                filterQueries = wp.filterQueries,
                                levelFilters = wp.levelFilters,
                                isReversed = wp.isReversed,
                                columns = wp.columns,
                                columnWidths = wp.columnWidths
                            )
                        },
                        activeWindowId = tp.activeWindowId
                    )
                },
                activeTabId = prefs.activeTabId ?: prefs.tabs.firstOrNull()?.id,
                isDarkMode = prefs.isDarkMode,
                isSidebarExpanded = prefs.isSidebarExpanded,
                recentFiles = prefs.recentFiles,
                recentDirectories = prefs.recentDirectories
            )
            _state.value = initialState
            
            // Reload logs for all windows
            initialState.tabs.forEach { tab ->
                tab.windows.forEach { window ->
                    if (window.sourceIds.isNotEmpty()) {
                        loadFilesIntoWindow(window.id, window.sourceIds)
                    }
                }
            }
        } else {
            _state.update { it.copy(
                isDarkMode = prefs.isDarkMode,
                isSidebarExpanded = prefs.isSidebarExpanded,
                recentFiles = prefs.recentFiles,
                recentDirectories = prefs.recentDirectories
            ) }
        }
    }

    fun handleIntent(intent: LogViewerIntent) {
        logger.debug { "Handling intent: ${intent::class.simpleName}" }
        when (intent) {
            is LogViewerIntent.LoadFiles -> {
                val activeWindowId = _state.value.activeTab?.activeWindow?.id
                if (activeWindowId != null) {
                    loadFilesIntoWindow(activeWindowId, intent.paths)
                    updateRecentItems(intent.paths)
                }
            }
            is LogViewerIntent.AddToWorkspace -> {
                val activeWindow = _state.value.activeTab?.activeWindow
                if (activeWindow != null) {
                    val currentPaths = activeWindow.sourceIds
                    val allPaths = currentPaths + intent.paths
                    loadFilesIntoWindow(activeWindow.id, allPaths)
                    updateRecentItems(intent.paths)
                }
            }
            is LogViewerIntent.SelectPath -> {
                _state.update { it.updateActiveWindow { window -> window.copy(filePath = intent.path) } }
                savePreferences()
            }
            LogViewerIntent.ClearLogs -> clearActiveWindow()
            LogViewerIntent.ToggleTheme -> {
                _state.update { it.copy(isDarkMode = !it.isDarkMode) }
                savePreferences()
            }
            LogViewerIntent.ToggleSidebar -> {
                _state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
                savePreferences()
            }
            is LogViewerIntent.AddFilterQuery -> {
                if (intent.query.isNotBlank()) {
                    _state.update { currentState ->
                        currentState.updateActiveWindow { window ->
                            if (!window.filterQueries.contains(intent.query)) {
                                window.copy(filterQueries = window.filterQueries + intent.query)
                            } else window
                        }
                    }
                    filterLogs(_state.value.activeTab?.activeWindow?.id)
                    savePreferences()
                }
            }
            is LogViewerIntent.RemoveFilterQuery -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = window.filterQueries - intent.query)
                    }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            LogViewerIntent.ClearFilterQueries -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = emptyList())
                    }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            is LogViewerIntent.ToggleLevel -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val newFilters = if (window.levelFilters.contains(intent.level)) {
                            window.levelFilters - intent.level
                        } else {
                            window.levelFilters + intent.level
                        }
                        window.copy(levelFilters = newFilters)
                    }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            LogViewerIntent.ToggleSortOrder -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(isReversed = !it.isReversed) }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            LogViewerIntent.AddTab -> addTab()
            is LogViewerIntent.CloseTab -> closeTab(intent.id)
            is LogViewerIntent.SwitchTab -> {
                _state.update { it.copy(activeTabId = intent.id) }
                savePreferences()
            }
            is LogViewerIntent.SelectEntry -> {
                _state.update { it.updateActiveWindow { window -> window.copy(selectedEntry = intent.entry) } }
            }
            LogViewerIntent.ShowOpenDialog -> _state.update { it.copy(pendingDialog = LogViewerState.DialogType.OPEN) }
            LogViewerIntent.ShowAddDialog -> _state.update { it.copy(pendingDialog = LogViewerState.DialogType.ADD) }
            LogViewerIntent.ShowRecentDialog -> _state.update { it.copy(pendingDialog = LogViewerState.DialogType.RECENT_ITEMS) }
            LogViewerIntent.DismissDialog -> _state.update { it.copy(pendingDialog = null) }
            
            // Split Management
            LogViewerIntent.SplitHorizontal -> splitHorizontal()
            is LogViewerIntent.CloseWindow -> closeWindow(intent.id)
            is LogViewerIntent.SwitchWindow -> switchWindow(intent.id)
            is LogViewerIntent.UpdateColumnWidth -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(columnWidths = window.columnWidths + (intent.column to intent.width))
                    }
                }
                savePreferences(debounce = true)
            }
        }
    }

    private fun addTab() {
        val newTabId = UUID.randomUUID().toString()
        val newWindowId = UUID.randomUUID().toString()
        val newTab = TabState(
            id = newTabId,
            title = "New Tab",
            windows = listOf(LogWindow(id = newWindowId)),
            activeWindowId = newWindowId
        )
        _state.update { it.copy(tabs = it.tabs + newTab, activeTabId = newTabId) }
        savePreferences()
    }

    private fun closeTab(id: String) {
        val tab = _state.value.tabs.find { it.id == id }
        tab?.windows?.forEach { window ->
            logJobs[window.id]?.cancel()
            logJobs.remove(window.id)
        }
        _state.update { currentState ->
            val remainingTabs = currentState.tabs.filter { it.id != id }
            val newTabs = if (remainingTabs.isEmpty()) {
                val newTabId = UUID.randomUUID().toString()
                val newWindowId = UUID.randomUUID().toString()
                listOf(TabState(
                    id = newTabId,
                    title = "Log View",
                    windows = listOf(LogWindow(id = newWindowId)),
                    activeWindowId = newWindowId
                ))
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
        savePreferences()
    }

    private fun splitHorizontal() {
        val activeTab = _state.value.activeTab ?: return
        val newWindowId = UUID.randomUUID().toString()
        val newWindow = LogWindow(id = newWindowId)
        
        _state.update { currentState ->
            currentState.updateActiveTab { tab ->
                tab.copy(
                    windows = tab.windows + newWindow,
                    activeWindowId = newWindowId
                )
            }
        }
        savePreferences()
    }

    private fun closeWindow(id: String) {
        logJobs[id]?.cancel()
        logJobs.remove(id)
        
        _state.update { currentState ->
            currentState.updateActiveTab { tab ->
                val remainingWindows = tab.windows.filter { it.id != id }
                if (remainingWindows.isEmpty()) tab // Cannot close the last window
                else {
                    val newActiveId = if (tab.activeWindowId == id) remainingWindows.last().id else tab.activeWindowId
                    tab.copy(windows = remainingWindows, activeWindowId = newActiveId)
                }
            }
        }
        savePreferences()
    }

    private fun switchWindow(id: String) {
        _state.update { currentState ->
            currentState.updateActiveTab { it.copy(activeWindowId = id) }
        }
        savePreferences()
    }

    private fun clearActiveWindow() {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        logJobs[activeWindow.id]?.cancel()
        _state.update { currentState ->
            currentState.updateActiveWindow { window ->
                window.copy(logs = emptyList(), filePath = "", sourceIds = emptyList())
            }
        }
        filterLogs(activeWindow.id)
        savePreferences()
    }

    private fun loadFilesIntoWindow(windowId: String, paths: List<String>) {
        logJobs[windowId]?.cancel()
        logJobs[windowId] = scope.launch {
            val fileName = if (paths.size == 1) File(paths[0]).name else "${paths.size} files"
            
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    tab.copy(windows = tab.windows.map { window ->
                        if (window.id == windowId) {
                            window.copy(
                                isLoading = true, 
                                error = null, 
                                filePath = paths.joinToString(", "), 
                                logs = emptyList(), 
                                sourceIds = paths
                            )
                        } else window
                    })
                })
            }
            
            // Update tab title if it's the only window or first window
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = fileName)
                    } else tab
                })
            }
            
            savePreferences()
            
            val flow = if (paths.size == 1) {
                val path = paths[0]
                val sampleLines = readSampleLines(path)
                val probeResult = heuristicProbe.detect(sampleLines)
                
                _state.update { currentState ->
                    currentState.copy(tabs = currentState.tabs.map { tab ->
                        tab.copy(windows = tab.windows.map { window ->
                            if (window.id == windowId) window.copy(columns = probeResult.columns) else window
                        })
                    })
                }
                
                logSource.observeLogs(LogFilePath(path), probeResult.parser)
            } else {
                val results = paths.map { path ->
                    val sampleLines = readSampleLines(path)
                    heuristicProbe.detect(sampleLines)
                }
                
                _state.update { currentState ->
                    currentState.copy(tabs = currentState.tabs.map { tab ->
                        tab.copy(windows = tab.windows.map { window ->
                            if (window.id == windowId) window.copy(columns = results.firstOrNull()?.columns ?: emptyList()) else window
                        })
                    })
                }
                
                val sources = paths.mapIndexed { index, path ->
                    Triple(logSource, LogFilePath(path), results[index].parser)
                }
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
                        _state.update { currentState ->
                            currentState.copy(tabs = currentState.tabs.map { tab ->
                                tab.copy(windows = tab.windows.map { window ->
                                    if (window.id == windowId) window.copy(isLoading = false, error = message) else window
                                })
                            })
                        }
                        _events.emit(LogViewerEvent.ShowError(message))
                    },
                    ifRight = { update ->
                        handleLogUpdate(windowId, update)
                    }
                )
            }
        }
    }

    private fun readSampleLines(path: String, limit: Int = 50): List<String> {
        return try {
            File(path).useLines { it.take(limit).toList() }
        } catch (e: Exception) {
            logger.warn { "Failed to read sample lines from $path: ${e.message}" }
            emptyList()
        }
    }

    private fun handleLogUpdate(windowId: String, update: LogUpdate) {
        _state.update { currentState ->
            currentState.copy(tabs = currentState.tabs.map { tab ->
                tab.copy(windows = tab.windows.map { window ->
                    if (window.id == windowId) {
                        when (update) {
                            is LogUpdate.Initial -> window.copy(isLoading = false, logs = update.entries)
                            is LogUpdate.Appended -> window.copy(logs = window.logs + update.entries)
                            LogUpdate.Reset -> window.copy(logs = emptyList())
                        }
                    } else window
                })
            })
        }
        filterLogs(windowId)
    }

    private fun filterLogs(windowId: String?) {
        if (windowId == null) return
        
        scope.launch(Dispatchers.Default) {
            val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return@launch
            
            val filtered = window.logs.filter { entry ->
                val matchesLevel = window.levelFilters.contains(entry.level)
                val matchesFilter = if (window.filterQueries.isEmpty()) {
                    true
                } else {
                    window.filterQueries.all { query ->
                        entry.content.value.contains(query, ignoreCase = true) ||
                        entry.timestamp.value.contains(query, ignoreCase = true)
                    }
                }
                matchesLevel && matchesFilter
            }
            
            val sorted = if (window.isReversed) filtered.reversed() else filtered
            
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    tab.copy(windows = tab.windows.map { 
                        if (it.id == windowId) it.copy(filteredLogs = sorted) else it 
                    })
                })
            }
        }
    }

    private fun updateRecentItems(paths: List<String>) {
        val files = paths.filter { File(it).isFile }
        val dirs = paths.filter { File(it).isDirectory }
        
        if (files.isEmpty() && dirs.isEmpty()) return

        _state.update { currentState ->
            val newRecentFiles = (files + currentState.recentFiles).distinct().take(50)
            val newRecentDirectories = (dirs + currentState.recentDirectories).distinct().take(50)
            
            currentState.copy(
                recentFiles = newRecentFiles,
                recentDirectories = newRecentDirectories
            ).also { savePreferences(it) }
        }
    }

    fun savePreferences(currentState: LogViewerState = _state.value, debounce: Boolean = false) {
        if (debounce) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(500)
                performSave(currentState)
            }
        } else {
            saveJob?.cancel()
            performSave(currentState)
        }
    }

    private fun performSave(currentState: LogViewerState) {
        val currentPrefs = prefsRepository.load()
        val newPrefs = currentPrefs.copy(
            isDarkMode = currentState.isDarkMode,
            isSidebarExpanded = currentState.isSidebarExpanded,
            recentFiles = currentState.recentFiles,
            recentDirectories = currentState.recentDirectories,
            tabs = currentState.tabs.map { tab ->
                TabPreference(
                    id = tab.id,
                    title = tab.title,
                    activeWindowId = tab.activeWindowId,
                    windows = tab.windows.map { window ->
                        WindowPreference(
                            id = window.id,
                            filePath = window.filePath,
                            sourceIds = window.sourceIds,
                            filterQueries = window.filterQueries,
                            levelFilters = window.levelFilters,
                            isReversed = window.isReversed,
                            columns = window.columns,
                            columnWidths = window.columnWidths
                        )
                    }
                )
            },
            activeTabId = currentState.activeTabId
        )
        prefsRepository.save(newPrefs)
    }
}
