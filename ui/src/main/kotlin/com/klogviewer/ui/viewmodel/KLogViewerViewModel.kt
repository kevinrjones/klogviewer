package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.*
import com.klogviewer.domain.parser.LogParser
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
    private val remoteFileSystem: RemoteFileSystem = SftpFileSystem()
) {
    private val _state = MutableStateFlow(KLogViewerState())
    val state: StateFlow<KLogViewerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<KLogViewerEvent>()
    val events: SharedFlow<KLogViewerEvent> = _events.asSharedFlow()

    private val logLoadingCoordinator = LogLoadingCoordinator(
        localFileSystem = localFileSystem,
        remoteFileSystem = remoteFileSystem,
        logSource = logSource,
        heuristicProbe = heuristicProbe,
        logSourceFactory = logSourceFactory,
        scope = scope,
        state = _state,
        onSavePreferences = { savePreferences() },
        onHandleLogUpdate = { windowId, update, sourceId -> handleLogUpdate(windowId, update, sourceId) },
        onShowError = { message -> _events.emit(KLogViewerEvent.ShowError(message)) }
    )

    private var saveJob: Job? = null

    private val recentItemsManager = RecentItemsManager(localFileSystem)
    private val sftpIntentHandler = SftpIntentHandler(
        remoteFileSystem = remoteFileSystem,
        scope = scope,
        state = _state,
        onSavePreferences = { savePreferences() },
        onLoadFiles = { windowId, paths -> logLoadingCoordinator.loadFilesIntoWindow(windowId, paths) },
        onConnectSftp = { windowId, name, host, port, user, auth, path ->
            logLoadingCoordinator.connectSftp(windowId, name, host, port, user, auth, path)
        },
        onConnectMultipleSftp = { windowId, config, paths ->
            logLoadingCoordinator.connectMultipleSftp(windowId, config, paths)
        },
        onConnectSftpDirectory = { windowId, config, path ->
            logLoadingCoordinator.connectSftpDirectory(windowId, config, path)
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
        logLoadingCoordinator.cancelAll()
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
                    logLoadingCoordinator.loadFilesIntoWindow(window.id, window.sourceIds, window.parserName)
                }
            }
        }
    }

    fun handleIntent(intent: KLogViewerIntent) {
        logger.debug { "Handling intent: ${intent::class.simpleName}" }
        when (intent) {
            is KLogViewerIntent.WorkspaceIntent -> handleWorkspaceIntent(intent)
            is KLogViewerIntent.UiToggleIntent -> handleUiToggleIntent(intent)
            is KLogViewerIntent.FilterIntent -> handleFilterIntent(intent)
            is KLogViewerIntent.TabWindowIntent -> handleTabWindowIntent(intent)
            is KLogViewerIntent.EntryIntent -> handleEntryIntent(intent)
            is KLogViewerIntent.SftpIntent -> handleSftpIntent(intent)
            is KLogViewerIntent.DialogIntent -> handleDialogIntent(intent)
            is KLogViewerIntent.RecentItemsIntent -> handleRecentItemsIntent(intent)
        }
    }

    private fun handleWorkspaceIntent(intent: KLogViewerIntent.WorkspaceIntent) {
        when (intent) {
            is KLogViewerIntent.LoadFiles -> {
                val activeWindowId = _state.value.activeTab?.activeWindow?.id
                if (activeWindowId != null) {
                    logLoadingCoordinator.loadFilesIntoWindow(activeWindowId, intent.paths)
                    _state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
                }
            }
            is KLogViewerIntent.AddToWorkspace -> {
                val activeWindow = _state.value.activeTab?.activeWindow
                if (activeWindow != null) {
                    val currentPaths = activeWindow.sourceIds
                    val allPaths = (currentPaths + intent.paths).distinct()
                    logLoadingCoordinator.loadFilesIntoWindow(activeWindow.id, allPaths)
                    _state.update { recentItemsManager.updateRecentItems(it, intent.paths) }
                }
            }
            is KLogViewerIntent.SelectPath -> {
                _state.update { it.updateActiveWindow { window -> window.copy(filePath = intent.path) } }
                savePreferences()
            }
            KLogViewerIntent.ClearLogs -> clearActiveWindow()
        }
    }

    private fun handleUiToggleIntent(intent: KLogViewerIntent.UiToggleIntent) {
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
        }
    }

    private fun handleFilterIntent(intent: KLogViewerIntent.FilterIntent) {
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
        }
    }

    private fun handleTabWindowIntent(intent: KLogViewerIntent.TabWindowIntent) {
        when (intent) {
            KLogViewerIntent.AddTab -> {
                _state.update { TabWindowController.addTab(it) }
                savePreferences()
            }
            is KLogViewerIntent.CloseTab -> {
                _state.update { TabWindowController.closeTab(it, intent.id) { windowId ->
                    logLoadingCoordinator.cancelWindowJob(windowId)
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
                    logLoadingCoordinator.cancelWindowJob(windowId)
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
            is KLogViewerIntent.ChangeParser -> {
                val window = _state.value.tabs.flatMap { it.windows }.find { it.id == intent.windowId }
                if (window != null && window.sourceIds.isNotEmpty()) {
                    logLoadingCoordinator.loadFilesIntoWindow(intent.windowId, window.sourceIds, intent.parserName)
                }
            }
        }
    }

    private fun handleEntryIntent(intent: KLogViewerIntent.EntryIntent) {
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
        }
    }

    private fun handleSftpIntent(intent: KLogViewerIntent.SftpIntent) {
        sftpIntentHandler.handle(intent)
    }

    private fun handleDialogIntent(intent: KLogViewerIntent.DialogIntent) {
        when (intent) {
            KLogViewerIntent.ShowOpenDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN, isAddMode = false) }
            KLogViewerIntent.ShowOpenDirectoryDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN_DIRECTORY, isAddMode = false) }
            KLogViewerIntent.ShowAddDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD, isAddMode = true) }
            KLogViewerIntent.ShowAddDirectoryDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD_DIRECTORY, isAddMode = true) }
            KLogViewerIntent.ShowAddSftpDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_ADD, isAddMode = true) }
            KLogViewerIntent.ShowRecentDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.RECENT_ITEMS) }
            KLogViewerIntent.ShowSftpDialog -> _state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_CONNECT, isAddMode = false) }
            KLogViewerIntent.DismissDialog -> _state.update { it.copy(pendingDialog = null) }
        }
    }

    private fun handleRecentItemsIntent(intent: KLogViewerIntent.RecentItemsIntent) {
        when (intent) {
            is KLogViewerIntent.RemoveRecentItem -> {
                _state.update { recentItemsManager.removeRecentItem(it, intent.path) }
                savePreferences()
            }
            KLogViewerIntent.ClearMissingRecentItems -> {
                _state.update { recentItemsManager.clearMissingRecentItems(it) }
                savePreferences()
            }
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
                logLoadingCoordinator.loadFilesIntoWindow(activeWindow.id, activeWindow.sourceIds, activeWindow.parserName)
            }
        } else {
            logLoadingCoordinator.cancelWindowJob(activeWindow.id)
        }
        
        savePreferences()
    }


    private fun clearActiveWindow() {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        logLoadingCoordinator.cancelWindowJob(activeWindow.id)
        _state.update { currentState ->
            currentState.updateActiveWindow { window ->
                window.copy(logs = emptyList(), filePath = "", sourceIds = emptyList())
            }
        }
        filterLogs(activeWindow.id)
        savePreferences()
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
            val filteredLogs = LogFilterService.filter(window)
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) { it.copy(filteredLogs = filteredLogs) }
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
