package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.core.parser.*
import com.klogviewer.core.source.DirectoryLogSource
import com.klogviewer.core.source.MergedLogSource
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.ui.mvi.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.UUID
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class KLogViewerViewModel(
    private val logSource: LogSource,
    private val prefsRepository: PreferencesRepository,
    val heuristicProbe: HeuristicProbe,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    private val _state = MutableStateFlow(KLogViewerState())
    val state: StateFlow<KLogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<KLogViewerEvent>()
    val events: SharedFlow<KLogViewerEvent> = _events.asSharedFlow()

    private val logJobs = mutableMapOf<String, Job>()
    private var saveJob: Job? = null
    
    fun clear() {
        scope.cancel()
    }

    init {
        val prefs = prefsRepository.load()
        if (prefs.tabs.isNotEmpty()) {
            val initialState = KLogViewerState(
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
                                isAutoScrollEnabled = wp.isAutoScrollEnabled,
                                showAnsiColors = wp.showAnsiColors,
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

    fun handleIntent(intent: KLogViewerIntent) {
        logger.debug { "Handling intent: ${intent::class.simpleName}" }
        when (intent) {
            is KLogViewerIntent.LoadFiles -> {
                val activeWindowId = _state.value.activeTab?.activeWindow?.id
                if (activeWindowId != null) {
                    loadFilesIntoWindow(activeWindowId, intent.paths)
                    updateRecentItems(intent.paths)
                }
            }
            is KLogViewerIntent.AddToWorkspace -> {
                val activeWindow = _state.value.activeTab?.activeWindow
                if (activeWindow != null) {
                    val currentPaths = activeWindow.sourceIds
                    val allPaths = currentPaths + intent.paths
                    loadFilesIntoWindow(activeWindow.id, allPaths)
                    updateRecentItems(intent.paths)
                }
            }
            is KLogViewerIntent.SelectPath -> {
                _state.update { it.updateActiveWindow { window -> window.copy(filePath = intent.path) } }
                savePreferences()
            }
            KLogViewerIntent.ClearLogs -> clearActiveWindow()
            KLogViewerIntent.ToggleTheme -> {
                _state.update { it.copy(isDarkMode = !it.isDarkMode) }
                savePreferences()
            }
            KLogViewerIntent.ToggleSidebar -> {
                _state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
                savePreferences()
            }
            is KLogViewerIntent.AddFilterQuery -> {
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
            is KLogViewerIntent.RemoveFilterQuery -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = window.filterQueries - intent.query)
                    }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            KLogViewerIntent.ClearFilterQueries -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        window.copy(filterQueries = emptyList())
                    }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            is KLogViewerIntent.ToggleLevel -> {
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
            KLogViewerIntent.ToggleAllLevels -> {
                _state.update { currentState ->
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
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            KLogViewerIntent.ToggleSortOrder -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(isReversed = !it.isReversed) }
                }
                filterLogs(_state.value.activeTab?.activeWindow?.id)
                savePreferences()
            }
            KLogViewerIntent.ToggleAutoScroll -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(isAutoScrollEnabled = !it.isAutoScrollEnabled) }
                }
                savePreferences()
            }
            KLogViewerIntent.ToggleAnsiColors -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { it.copy(showAnsiColors = !it.showAnsiColors) }
                }
                savePreferences()
            }
            KLogViewerIntent.AddTab -> addTab()
            is KLogViewerIntent.CloseTab -> closeTab(intent.id)
            is KLogViewerIntent.SwitchTab -> {
                _state.update { it.copy(activeTabId = intent.id) }
                savePreferences()
            }
            is KLogViewerIntent.SelectEntry -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window -> 
                        val index = window.filteredLogs.indexOf(intent.entry)
                        window.copy(
                            selectedEntry = intent.entry,
                            selectedIndices = if (intent.entry != null && index != -1) setOf(index) else emptySet(),
                            lastSelectedIndex = if (index != -1) index else null
                        ) 
                    }
                }
            }
            is KLogViewerIntent.ToggleEntrySelection -> {
                _state.update { currentState ->
                    currentState.updateActiveWindow { window ->
                        val newIndices = when {
                            intent.isShiftPressed && window.lastSelectedIndex != null -> {
                                val start = minOf(window.lastSelectedIndex, intent.index)
                                val end = maxOf(window.lastSelectedIndex, intent.index)
                                window.selectedIndices + (start..end).toSet()
                            }
                            intent.isMetaPressed -> {
                                if (window.selectedIndices.contains(intent.index)) {
                                    window.selectedIndices - intent.index
                                } else {
                                    window.selectedIndices + intent.index
                                }
                            }
                            else -> setOf(intent.index)
                        }
                        window.copy(
                            selectedIndices = newIndices,
                            lastSelectedIndex = intent.index,
                            selectedEntry = if (newIndices.size == 1) window.filteredLogs.getOrNull(intent.index) else window.selectedEntry
                        )
                    }
                }
            }
            KLogViewerIntent.CopySelected -> copySelectedToClipboard()
            KLogViewerIntent.ShowOpenDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN) }
            KLogViewerIntent.ShowOpenDirectoryDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN_DIRECTORY) }
            KLogViewerIntent.ShowAddDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD) }
            KLogViewerIntent.ShowRecentDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.RECENT_ITEMS) }
            KLogViewerIntent.DismissDialog -> _state.update { it.copy(pendingDialog = null, missingPath = null) }
            is KLogViewerIntent.RemoveRecentItem -> {
                _state.update { currentState ->
                    currentState.copy(
                        recentFiles = currentState.recentFiles - intent.path,
                        recentDirectories = currentState.recentDirectories - intent.path
                    )
                }
                savePreferences()
            }
            KLogViewerIntent.ClearMissingRecentItems -> {
                _state.update { currentState ->
                    currentState.copy(
                        recentFiles = currentState.recentFiles.filter { File(it).exists() },
                        recentDirectories = currentState.recentDirectories.filter { File(it).exists() }
                    )
                }
                savePreferences()
            }
            
            // Split Management
            KLogViewerIntent.SplitHorizontal -> splitHorizontal()
            is KLogViewerIntent.CloseWindow -> closeWindow(intent.id)
            is KLogViewerIntent.SwitchWindow -> switchWindow(intent.id)
            is KLogViewerIntent.UpdateColumnWidth -> {
                _state.update { currentState ->
                    currentState.updateWindow(intent.windowId) { window ->
                        window.copy(columnWidths = window.columnWidths + (intent.column to intent.width))
                    }
                }
                savePreferences(debounce = true)
            }
            is KLogViewerIntent.ChangeParser -> {
                val window = _state.value.tabs.flatMap { it.windows }.find { it.id == intent.windowId }
                if (window != null && window.sourceIds.isNotEmpty()) {
                    loadFilesIntoWindow(intent.windowId, window.sourceIds, intent.parserName)
                }
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

    private fun loadFilesIntoWindow(windowId: String, paths: List<String>, overrideParserName: String? = null) {
        val missingPaths = paths.filter { !File(it).exists() }
        if (missingPaths.isNotEmpty()) {
            _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.MISSING_FILE, missingPath = missingPaths.first()) }
            return
        }

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
                if (File(path).isDirectory) {
                    DirectoryLogSource(logSource, heuristicProbe).observeLogs(LogFilePath(path))
                } else {
                    val sampleLines = readSampleLines(path)
                    val probeResult = if (overrideParserName != null) {
                        getParserResultByName(overrideParserName, sampleLines)
                    } else {
                        heuristicProbe.detect(sampleLines)
                    }
                    
                    _state.update { currentState ->
                        currentState.copy(tabs = currentState.tabs.map { tab ->
                            tab.copy(windows = tab.windows.map { window ->
                                if (window.id == windowId) {
                                    window.copy(
                                        columns = probeResult.columns,
                                        parserName = probeResult.parserName
                                    )
                                } else window
                            })
                        })
                    }
                    
                    logSource.observeLogs(LogFilePath(path), probeResult.parser)
                }
            } else {
                val results = paths.map { path ->
                    if (File(path).isDirectory) {
                        null
                    } else {
                        val sampleLines = readSampleLines(path)
                        if (overrideParserName != null) {
                            getParserResultByName(overrideParserName, sampleLines)
                        } else {
                            heuristicProbe.detect(sampleLines)
                        }
                    }
                }
                
                _state.update { currentState ->
                    currentState.copy(tabs = currentState.tabs.map { tab ->
                        tab.copy(windows = tab.windows.map { window ->
                            if (window.id == windowId) {
                                window.copy(
                                    columns = results.firstNotNullOfOrNull { it?.columns } ?: emptyList(),
                                    parserName = if (results.size > 1 && overrideParserName == null) "Multiple" else (overrideParserName ?: results.firstOrNull()?.parserName ?: "Auto")
                                )
                            } else window
                        })
                    })
                }
                
                val sources = paths.mapIndexed { index, path ->
                    if (File(path).isDirectory) {
                        DirectoryLogSource(logSource, heuristicProbe).observeLogs(LogFilePath(path))
                    } else {
                        logSource.observeLogs(LogFilePath(path), results[index]?.parser)
                    }
                }
                
                // If we have mixed flows, MergedLogSource needs to be updated or we use flow.merge()
                // For now, let's just use merge() if any is a directory.
                if (paths.any { File(it).isDirectory }) {
                    sources.merge()
                } else {
                    val legacySources = paths.mapIndexed { index, path ->
                        Triple(logSource, LogFilePath(path), results[index]?.parser)
                    }
                    MergedLogSource(legacySources).observeMerged()
                }
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
                        _events.emit(KLogViewerEvent.ShowError(message))
                    },
                    ifRight = { update ->
                        handleLogUpdate(windowId, update)
                    }
                )
            }
        }
    }

    private fun getParserResultByName(name: String, sampleLines: List<String>): ProbeResult {
        return when (name) {
            "JSON" -> {
                val detected = heuristicProbe.detect(sampleLines)
                if (detected.parser is JsonLogParser) detected
                else ProbeResult(JsonLogParser(), "JSON", listOf("Timestamp", "Level", "Content"))
            }
            "logfmt" -> ProbeResult(LogfmtParser(), "logfmt", listOf("Timestamp", "Level", "Content"))
            "Simple" -> ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
            else -> {
                val template = heuristicProbe.registry.getTemplate(name)
                if (template != null) ProbeResult(TemplateLogParser(template), template.name, template.columns)
                else ProbeResult(SimpleLogParser(), "Simple", listOf("Timestamp", "Level", "Content"))
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
                        val newLogs = when (update) {
                            is LogUpdate.Initial -> update.entries
                            is LogUpdate.Appended -> window.logs + update.entries
                            LogUpdate.Reset -> emptyList()
                            is LogUpdate.SourceMissing -> window.logs
                        }
                        
                        val newMissingSourceIds = when (update) {
                            is LogUpdate.SourceMissing -> window.missingSourceIds + update.sourceId
                            is LogUpdate.Initial -> {
                                val incoming = update.entries.mapNotNull { it.sourceId }.toSet()
                                window.missingSourceIds - incoming
                            }
                            is LogUpdate.Appended -> {
                                val incoming = update.entries.mapNotNull { it.sourceId }.toSet()
                                window.missingSourceIds - incoming
                            }
                            LogUpdate.Reset -> emptySet()
                        }
                        
                        // Extract unique source IDs from the logs to ensure badges are shown for all discovered files
                        val discoveredSourceIds = newLogs.mapNotNull { it.sourceId }.distinct().filter { it.isNotEmpty() }
                        val mergedSourceIds = (window.sourceIds + discoveredSourceIds).distinct()
                        
                        window.copy(
                            isLoading = false, 
                            logs = newLogs,
                            sourceIds = mergedSourceIds,
                            missingSourceIds = newMissingSourceIds
                        )
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

    fun savePreferences(currentState: KLogViewerState = _state.value, debounce: Boolean = false) {
        if (debounce) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(500.milliseconds)
                performSave(currentState)
            }
        } else {
            saveJob?.cancel()
            performSave(currentState)
        }
    }

    private fun performSave(currentState: KLogViewerState) {
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
                            isAutoScrollEnabled = window.isAutoScrollEnabled,
                            showAnsiColors = window.showAnsiColors,
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

    private fun copySelectedToClipboard() {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        val indices = activeWindow.selectedIndices.sorted()
        if (indices.isEmpty()) return

        val textToCopy = indices.mapNotNull { activeWindow.filteredLogs.getOrNull(it) }
            .joinToString("\n") { it.content.value }

        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val selection = StringSelection(textToCopy)
            clipboard.setContents(selection, null)
            logger.info { "Copied ${indices.size} lines to clipboard" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to copy to clipboard" }
        }
    }
}
