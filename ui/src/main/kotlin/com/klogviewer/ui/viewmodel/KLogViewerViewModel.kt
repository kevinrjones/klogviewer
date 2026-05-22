package com.klogviewer.ui.viewmodel

import arrow.core.*
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.*
import com.klogviewer.core.parser.*
import com.klogviewer.core.source.*
import com.klogviewer.core.repository.*
import com.klogviewer.ui.mvi.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

class KLogViewerViewModel(
    private val logSource: LogSource,
    private val prefsRepository: PreferencesRepository,
    val heuristicProbe: HeuristicProbe,
    private val logSourceFactory: LogSourceFactory = DefaultLogSourceFactory(),
    private val clipboard: Clipboard = AwtClipboard(),
    val localFileSystem: LocalFileSystem = JavaLocalFileSystem(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val remoteFileSystem: RemoteFileSystem = SftpFileSystem(),
    private val sftpDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _state = MutableStateFlow(KLogViewerState())
    val state: StateFlow<KLogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<KLogViewerEvent>()
    val events: SharedFlow<KLogViewerEvent> = _events.asSharedFlow()

    private val logJobs = mutableMapOf<String, Job>()
    private var saveJob: Job? = null

    private val recentItemsManager = RecentItemsManager(localFileSystem)
    private val sftpIntentHandler = SftpIntentHandler(
        remoteFileSystem = remoteFileSystem,
        scope = scope,
        state = _state,
        onSavePreferences = { savePreferences() },
        onLoadFiles = { windowId, paths -> loadFilesIntoWindow(windowId, paths) },
        onConnectSftp = { windowId, name, host, port, user, auth, path ->
            connectSftp(windowId, name, host, port, user, auth, path)
        },
        onConnectMultipleSftp = { windowId, config, paths ->
            connectMultipleSftp(windowId, config, paths)
        },
        onConnectSftpDirectory = { windowId, config, path ->
            connectSftpDirectory(windowId, config, path)
        },
        onHandleBrowse = { config, path ->
            scope.launch {
                _state.update { it.copy(isRemoteLoading = true, pendingDialog = KLogViewerState.DialogType.SFTP_BROWSE, currentSftpConfig = config) }
                val result = remoteFileSystem.listFiles(config, path)
                _state.update {
                    it.copy(
                        isRemoteLoading = false,
                        remoteFiles = result.getOrNull()?.filter { f -> f.name != "." && f.name != ".." } ?: emptyList(),
                        remoteBrowsePath = path
                    )
                }
            }
        }
    )
    
    fun clear() {
        logJobs.values.forEach { it.cancel() }
        saveJob?.cancel()
        scope.cancel()
    }

    init {
        restoreStateFromPreferences()
    }

    private fun restoreStateFromPreferences() {
        val prefs = prefsRepository.load()
        val restoredState = PreferencesStateMapper.toState(prefs)
        _state.value = restoredState
        
        // Reload logs for all windows that are connected
        restoredState.tabs.forEach { tab ->
            tab.windows.forEach { window ->
                if (window.sourceIds.isNotEmpty() && window.isConnected) {
                    loadFilesIntoWindow(window.id, window.sourceIds, window.parserName)
                }
            }
        }
    }

    fun handleIntent(intent: KLogViewerIntent) {
        logger.debug { "Handling intent: ${intent::class.simpleName}" }
        when (intent) {
            is KLogViewerIntent.LoadFiles,
            is KLogViewerIntent.AddToWorkspace,
            is KLogViewerIntent.SelectPath,
            KLogViewerIntent.ClearLogs -> handleWorkspaceIntent(intent)

            KLogViewerIntent.ToggleTheme,
            KLogViewerIntent.ToggleSidebar,
            KLogViewerIntent.ToggleSortOrder,
            KLogViewerIntent.ToggleAutoScroll,
            KLogViewerIntent.ToggleAnsiColors,
            KLogViewerIntent.ToggleConnection -> handleUiToggleIntent(intent)

            is KLogViewerIntent.AddFilterQuery,
            is KLogViewerIntent.RemoveFilterQuery,
            KLogViewerIntent.ClearFilterQueries,
            is KLogViewerIntent.ToggleLevel,
            KLogViewerIntent.ToggleAllLevels -> handleFilterIntent(intent)

            KLogViewerIntent.AddTab,
            is KLogViewerIntent.CloseTab,
            is KLogViewerIntent.SwitchTab,
            KLogViewerIntent.SplitHorizontal,
            is KLogViewerIntent.CloseWindow,
            is KLogViewerIntent.SwitchWindow,
            is KLogViewerIntent.UpdateColumnWidth -> handleTabWindowIntent(intent)

            is KLogViewerIntent.SelectEntry,
            is KLogViewerIntent.ToggleEntrySelection,
            KLogViewerIntent.CopySelected -> handleEntryIntent(intent)

            is KLogViewerIntent.ConnectSftp,
            is KLogViewerIntent.ConnectMultipleSftp,
            is KLogViewerIntent.ConnectSftpDirectory,
            is KLogViewerIntent.BrowseSftp,
            is KLogViewerIntent.NavigateRemote,
            is KLogViewerIntent.SaveSftpConnection,
            is KLogViewerIntent.DeleteSftpConnection -> handleSftpIntent(intent)

            KLogViewerIntent.ShowOpenDialog,
            KLogViewerIntent.ShowOpenDirectoryDialog,
            KLogViewerIntent.ShowAddDialog,
            KLogViewerIntent.ShowAddDirectoryDialog,
            KLogViewerIntent.ShowAddSftpDialog,
            KLogViewerIntent.ShowRecentDialog,
            KLogViewerIntent.ShowSftpDialog,
            KLogViewerIntent.DismissDialog -> handleDialogIntent(intent)

            is KLogViewerIntent.RemoveRecentItem,
            KLogViewerIntent.ClearMissingRecentItems -> handleRecentItemsIntent(intent)

            is KLogViewerIntent.ChangeParser -> {
                val window = _state.value.tabs.flatMap { it.windows }.find { it.id == intent.windowId }
                if (window != null && window.sourceIds.isNotEmpty()) {
                    loadFilesIntoWindow(intent.windowId, window.sourceIds, intent.parserName)
                }
            }
        }
    }

    private fun handleWorkspaceIntent(intent: KLogViewerIntent) {
        when (intent) {
            is KLogViewerIntent.LoadFiles -> {
                val activeWindowId = _state.value.activeTab?.activeWindow?.id
                if (activeWindowId != null) {
                    loadFilesIntoWindow(activeWindowId, intent.paths)
                    _state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
                }
            }
            is KLogViewerIntent.AddToWorkspace -> {
                val activeWindow = _state.value.activeTab?.activeWindow
                if (activeWindow != null) {
                    val currentPaths = activeWindow.sourceIds
                    val allPaths = (currentPaths + intent.paths).distinct()
                    loadFilesIntoWindow(activeWindow.id, allPaths)
                    _state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
                }
            }
            is KLogViewerIntent.SelectPath -> {
                _state.update { it.updateActiveWindow { window -> window.copy(filePath = intent.path) } }
                savePreferences()
            }
            KLogViewerIntent.ClearLogs -> clearActiveWindow()
            else -> {}
        }
    }

    private fun handleUiToggleIntent(intent: KLogViewerIntent) {
        when (intent) {
            KLogViewerIntent.ToggleTheme -> {
                _state.update { it.copy(isDarkMode = !it.isDarkMode) }
                savePreferences()
            }
            KLogViewerIntent.ToggleSidebar -> {
                _state.update { it.copy(isSidebarExpanded = !it.isSidebarExpanded) }
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
            KLogViewerIntent.ToggleConnection -> toggleConnection()
            else -> {}
        }
    }

    private fun handleFilterIntent(intent: KLogViewerIntent) {
        when (intent) {
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
            else -> {}
        }
    }

    private fun handleTabWindowIntent(intent: KLogViewerIntent) {
        when (intent) {
            KLogViewerIntent.AddTab -> {
                _state.update { TabWindowController.addTab(it) }
                savePreferences()
            }
            is KLogViewerIntent.CloseTab -> {
                _state.update { TabWindowController.closeTab(it, intent.id) { windowId ->
                    logJobs[windowId]?.cancel()
                    logJobs.remove(windowId)
                } }
                savePreferences()
            }
            is KLogViewerIntent.SwitchTab -> {
                _state.update { it.copy(activeTabId = intent.id) }
                savePreferences()
            }
            KLogViewerIntent.SplitHorizontal -> {
                _state.update { TabWindowController.splitHorizontal(it) }
                savePreferences()
            }
            is KLogViewerIntent.CloseWindow -> {
                _state.update { TabWindowController.closeWindow(it, intent.id) { windowId ->
                    logJobs[windowId]?.cancel()
                    logJobs.remove(windowId)
                } }
                savePreferences()
            }
            is KLogViewerIntent.SwitchWindow -> {
                _state.update { TabWindowController.switchWindow(it, intent.id) }
                savePreferences()
            }
            is KLogViewerIntent.UpdateColumnWidth -> {
                _state.update { currentState ->
                    currentState.updateWindow(intent.windowId) { window ->
                        window.copy(columnWidths = window.columnWidths + (intent.column to intent.width))
                    }
                }
                savePreferences(debounce = true)
            }
            else -> {}
        }
    }

    private fun handleEntryIntent(intent: KLogViewerIntent) {
        when (intent) {
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
            else -> {}
        }
    }

    private fun handleSftpIntent(intent: KLogViewerIntent) {
        if (intent is KLogViewerIntent.SftpIntent) {
            sftpIntentHandler.handle(intent)
        }
    }

    private fun handleDialogIntent(intent: KLogViewerIntent) {
        when (intent) {
            KLogViewerIntent.ShowOpenDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN, isAddMode = false) }
            KLogViewerIntent.ShowOpenDirectoryDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN_DIRECTORY, isAddMode = false) }
            KLogViewerIntent.ShowAddDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD, isAddMode = true) }
            KLogViewerIntent.ShowAddDirectoryDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD_DIRECTORY, isAddMode = true) }
            KLogViewerIntent.ShowAddSftpDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_ADD, isAddMode = true) }
            KLogViewerIntent.ShowRecentDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.RECENT_ITEMS) }
            KLogViewerIntent.ShowSftpDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_CONNECT, isAddMode = false) }
            KLogViewerIntent.DismissDialog -> _state.update { it.copy(pendingDialog = null) }
            else -> {}
        }
    }

    private fun handleRecentItemsIntent(intent: KLogViewerIntent) {
        when (intent) {
            is KLogViewerIntent.RemoveRecentItem -> {
                _state.update { recentItemsManager.removeRecentItem(it, intent.path) }
                savePreferences()
            }
            KLogViewerIntent.ClearMissingRecentItems -> {
                _state.update { recentItemsManager.clearMissingRecentItems(it) }
                savePreferences()
            }
            else -> {}
        }
    }

    private fun toggleConnection() {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        val newConnected = !activeWindow.isConnected
        
        _state.update { currentState ->
            currentState.updateActiveWindow { it.copy(isConnected = newConnected) }
        }
        
        if (newConnected) {
            if (activeWindow.sourceIds.isNotEmpty()) {
                loadFilesIntoWindow(activeWindow.id, activeWindow.sourceIds, activeWindow.parserName)
            }
        } else {
            logJobs[activeWindow.id]?.cancel()
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

    private fun connectSftpDirectory(windowId: String, config: SftpConfig, path: String, parserName: String? = null) {
        val oldJob = logJobs[windowId]
        
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            val sftpUri = SftpUri(config.username.value, config.host.value, config.port.value, path, isDirectory = true)
            val sourceId = sftpUri.toString()
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = sourceId,
                        sourceIds = listOf(sourceId),
                        logs = emptyList(),
                        parserName = parserName,
                        columns = listOf("Timestamp", "Level", "Content"),
                        isDirectory = true
                    )
                }
            }
            savePreferences()

            // Update tab title
            val title = path.substringAfterLast('/').ifEmpty { path }
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = title)
                    } else tab
                })
            }
            
            val sftpDirectorySource = logSourceFactory.createSftpDirectorySource(config, remoteFileSystem)
            val parser = parserName?.let { getParserResultByName(it, emptyList()).parser }
            
            sftpDirectorySource.observeLogs(LogFilePath(path), parser).collect { result ->
                result.fold(
                    { error ->
                        val sid = error.sourceId ?: sourceId
                        _state.update { it.updateWindow(windowId) { w -> 
                            w.copy(
                                error = error.message, 
                                isLoading = false, 
                                missingSourceIds = w.missingSourceIds + sid
                            ) 
                        } }
                    },
                    { update ->
                        handleLogUpdate(windowId, update, sourceId)
                    }
                )
            }
        }
    }

    private fun connectMultipleSftp(windowId: String, config: SftpConfig, paths: List<String>) {
        val oldJob = logJobs[windowId]
        
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            val sourceId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}${if (paths.size == 1) paths[0] else " (${paths.size} files)"}"
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = sourceId,
                        sourceIds = paths.map { "sftp://${config.username.value}@${config.host.value}:${config.port.value}$it" },
                        logs = emptyList(),
                        columns = listOf("Timestamp", "Level", "Content")
                    )
                }
            }
            savePreferences()

            // Update tab title
            val title = if (paths.size == 1) paths[0].substringAfterLast('/') else "${paths.size} remote files"
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = title)
                    } else tab
                })
            }
            
            val parser = SimpleLogParser()
            
            // Create a flow for each path and merge them
            val flows = paths.map { path ->
                val source = logSourceFactory.createSftpSource(config)
                val sId = "sftp://${config.username.value}@${config.host.value}:${config.port.value}$path"
                source.observeLogs(LogFilePath(path), parser).map { result ->
                    result.fold(
                        { l -> Pair(l, sId).left() },
                        { r -> Pair(r, sId).right() }
                    )
                }
            }
            
            flows.merge().collect { result ->
                result.fold(
                    { pair ->
                        val failure = pair.first
                        val sId = pair.second
                        _state.update { it.updateWindow(windowId) { w -> 
                            val sid = failure.sourceId ?: sId
                            val isCritical = sid == w.filePath
                            val newError = if (isCritical) failure.message else w.error
                            w.copy(
                                error = newError, 
                                isLoading = false,
                                missingSourceIds = w.missingSourceIds + sid
                            ) 
                        } }
                    },
                    { pair ->
                        val update = pair.first
                        val sId = pair.second
                        handleLogUpdate(windowId, update, sId)
                    }
                )
            }
        }
    }

    private fun findSftpConfig(uri: String): SftpConfig? {
        val sftpUri = SftpUri.parse(uri) ?: return null
        val found = _state.value.sftpConnections.find {
            it.username.value == sftpUri.username &&
            it.host.value == sftpUri.host &&
            it.port.value == sftpUri.port
        }
        return found
    }

    private fun connectSftp(windowId: String, name: String, host: String, port: Int, user: String, auth: SftpAuth, path: String, parserName: String? = null) {
        val config = SftpConfig(name, Host(host), Port(port), Username(user), auth, path)
        val sftpSource = logSourceFactory.createSftpSource(config)
        
        val oldJob = logJobs[windowId]
        
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            val sftpUri = SftpUri(user, host, port, path, isDirectory = false)
            val sourceId = sftpUri.toString()
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) { window ->
                    window.copy(
                        isLoading = true,
                        error = null,
                        filePath = sourceId,
                        sourceIds = listOf(sourceId),
                        logs = emptyList(),
                        parserName = parserName,
                        columns = listOf("Timestamp", "Level", "Content")
                    )
                }
            }
            savePreferences()

            // Update tab title if it's the only window or first window
            val fileName = path.substringAfterLast('/')
            _state.update { currentState ->
                currentState.copy(tabs = currentState.tabs.map { tab ->
                    if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) {
                        tab.copy(title = fileName)
                    } else tab
                })
            }
            
            val parser = SimpleLogParser()
            
            sftpSource.observeLogs(LogFilePath(path), parser)
                .collect { result ->
                    result.fold(
                        { error ->
                            _state.update { it.updateWindow(windowId) { w -> 
                                w.copy(
                                    error = error.message, 
                                    isLoading = false,
                                    missingSourceIds = w.missingSourceIds + sourceId
                                ) 
                            } }
                        },
                        { update ->
                            handleLogUpdate(windowId, update, sourceId)
                        }
                    )
                }
        }
    }

    private fun loadFilesIntoWindow(windowId: String, paths: List<String>, overrideParserName: String? = null) {
        val filteredPaths = filterRedundantPaths(paths)
        if (handleSingleSftpPath(windowId, filteredPaths, overrideParserName)) return

        val oldJob = logJobs[windowId]
        logJobs[windowId] = scope.launch {
            oldJob?.cancelAndJoin()
            
            updateWindowStateForLoading(windowId, filteredPaths)
            savePreferences()
            
            val results = performHeuristicDetection(filteredPaths, overrideParserName)
            updateWindowStateWithParserResults(windowId, results, overrideParserName)
            
            val flows = createLogFlows(filteredPaths, results)
            val flow = if (flows.size == 1) flows[0] else flows.merge()
            
            flow.collect { result ->
                result.fold(
                    ifLeft = { (failure, path) -> handleLogLoadingFailure(windowId, path, failure) },
                    ifRight = { (update, sourceId) -> handleLogUpdate(windowId, update, sourceId) }
                )
            }
        }
    }

    private fun handleSingleSftpPath(windowId: String, paths: List<String>, overrideParserName: String?): Boolean {
        if (paths.size != 1 || !paths[0].startsWith("sftp://")) return false
        
        val uri = paths[0]
        val config = findSftpConfig(uri)
        val sftpUri = SftpUri.parse(uri)
        
        if (config != null && sftpUri != null) {
            if (sftpUri.isDirectory) {
                connectSftpDirectory(windowId, config, sftpUri.path, overrideParserName)
            } else {
                connectSftp(windowId, config.name, config.host.value, config.port.value, config.username.value, config.auth, sftpUri.path, overrideParserName)
            }
            return true
        } else if (sftpUri != null) {
            _state.update { it.updateWindow(windowId) { logWindow ->
                logWindow.copy(
                    filePath = uri,
                    sourceIds = listOf(uri),
                    missingSourceIds = setOf(uri),
                    error = "SFTP connection not found in preferences"
                )
            } }
            return true
        }
        return false
    }

    private fun updateWindowStateForLoading(windowId: String, paths: List<String>) {
        val isDir = paths.size == 1 && localFileSystem.isDirectory(paths[0])
        val fileName = if (paths.size == 1) localFileSystem.getName(paths[0]) else "${paths.size} files"
        
        _state.update { currentState ->
            currentState.copy(tabs = currentState.tabs.map { tab ->
                val newWindows = tab.windows.map { window ->
                    if (window.id == windowId) {
                        window.copy(
                            isLoading = true, 
                            error = null, 
                            filePath = paths.joinToString(", "), 
                            logs = emptyList(), 
                            sourceIds = paths,
                            isDirectory = isDir
                        )
                    } else window
                }
                val newTitle = if (tab.windows.any { it.id == windowId } && tab.windows.size <= 1) fileName else tab.title
                tab.copy(windows = newWindows, title = newTitle)
            })
        }
    }

    private fun performHeuristicDetection(paths: List<String>, overrideParserName: String?): List<ProbeResult?> {
        return paths.map { path ->
            if (path.startsWith("sftp://") || (localFileSystem.exists(path) && localFileSystem.isDirectory(path))) {
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
    }

    private fun updateWindowStateWithParserResults(windowId: String, results: List<ProbeResult?>, overrideParserName: String?) {
        _state.update { currentState ->
            currentState.updateWindow(windowId) { window ->
                window.copy(
                    columns = results.firstNotNullOfOrNull { it?.columns } ?: listOf("Timestamp", "Level", "Content"),
                    parserName = if (results.size > 1 && overrideParserName == null) "Multiple" else (overrideParserName ?: results.firstOrNull()?.parserName ?: "Auto")
                )
            }
        }
    }

    private fun createLogFlows(paths: List<String>, results: List<ProbeResult?>): List<Flow<Either<Pair<LogFailure, String>, Pair<LogUpdate, String>>>> {
        return paths.mapIndexed { index, path ->
            val flow = when {
                path.startsWith("sftp://") -> createSftpLogFlow(path)
                localFileSystem.isDirectory(path) -> DirectoryLogSource(logSource, heuristicProbe).observeLogs(LogFilePath(path))
                else -> logSource.observeLogs(LogFilePath(path), results[index]?.parser)
            }
            flow.map { result ->
                result.fold(
                    { failure -> Pair(failure, path).left() },
                    { update -> Pair(update, path).right() }
                )
            }
        }
    }

    private fun createSftpLogFlow(path: String): Flow<Either<LogFailure, LogUpdate>> {
        val config = findSftpConfig(path)
        val sftpUri = SftpUri.parse(path)
        return if (config != null && sftpUri != null) {
            if (sftpUri.isDirectory) {
                logSourceFactory.createSftpDirectorySource(config, remoteFileSystem).observeLogs(LogFilePath(sftpUri.path))
            } else {
                logSourceFactory.createSftpSource(config).observeLogs(LogFilePath(sftpUri.path))
            }
        } else {
            flowOf(LogFailure.FileError("SFTP connection not found for $path", sourceId = path).left())
        }
    }

    private suspend fun handleLogLoadingFailure(windowId: String, path: String, failure: LogFailure) {
        val message = failure.message
        logger.error { "Failed to load logs from $path: $message" }
        _state.update { currentState ->
            currentState.updateWindow(windowId) { window ->
                val sourceId = failure.sourceId ?: path
                val newMissing = window.missingSourceIds + sourceId
                val isCritical = sourceId == window.filePath || path == window.filePath
                val newError = if (isCritical) message else window.error
                window.copy(isLoading = false, error = newError, missingSourceIds = newMissing)
            }
        }
        val currentWindow = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId }
        if (currentWindow?.error != null || currentWindow?.logs?.isEmpty() == true) {
            _events.emit(KLogViewerEvent.ShowError(message))
        }
    }

    private fun filterRedundantPaths(paths: List<String>): List<String> {
        val directories = paths.filter { path ->
            if (path.startsWith("sftp://")) {
                SftpUri.parse(path)?.isDirectory == true
            } else {
                localFileSystem.isDirectory(path)
            }
        }
        
        if (directories.isEmpty()) return paths

        return paths.filter { path ->
            val sftpUri = SftpUri.parse(path)
            val isDir = sftpUri?.isDirectory == true || localFileSystem.isDirectory(path)
            if (isDir) return@filter true
            
            !directories.any { dir ->
                if (path.startsWith("sftp://") && dir.startsWith("sftp://")) {
                    val dirUri = SftpUri.parse(dir)
                    if (sftpUri != null && dirUri != null) {
                        sftpUri.username == dirUri.username &&
                        sftpUri.host == dirUri.host &&
                        sftpUri.port == dirUri.port &&
                        sftpUri.path.startsWith(dirUri.path) &&
                        sftpUri.path != dirUri.path
                    } else false
                } else if (!path.startsWith("sftp://") && !dir.startsWith("sftp://")) {
                    path.startsWith(dir) && path != dir
                } else false
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
            localFileSystem.readLines(path, limit)
        } catch (e: Exception) {
            logger.warn { "Failed to read sample lines from $path: ${e.message}" }
            emptyList()
        }
    }

    private fun handleLogUpdate(windowId: String, update: LogUpdate, sourceId: String? = null) {
        _state.update { currentState ->
            currentState.updateWindow(windowId) { window ->
                LogUpdateReducer.reduce(window, update, sourceId)
            }
        }
        filterLogs(windowId)
    }

    private fun filterLogs(windowId: String?) {
        if (windowId == null) return
        
        scope.launch(Dispatchers.Default) {
            val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return@launch
            val filteredWindow = LogFilterService.filter(window)
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) { filteredWindow }
            }
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
        val newPrefs = PreferencesStateMapper.toPreferences(currentState, currentPrefs)
        prefsRepository.save(newPrefs)
    }

    private fun copySelectedToClipboard() {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        val indices = activeWindow.selectedIndices.sorted()
        if (indices.isEmpty()) return

        val textToCopy = indices.mapNotNull { activeWindow.filteredLogs.getOrNull(it) }
            .joinToString("\n") { it.content.value }

        try {
            clipboard.copy(textToCopy)
            logger.info { "Copied ${indices.size} lines to clipboard" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to copy to clipboard" }
        }
    }
}
