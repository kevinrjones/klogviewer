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
    
    private val workspaceIntentHandler = WorkspaceIntentHandler(
        state = _state,
        logLoadingCoordinator = logLoadingCoordinator,
        recentItemsManager = recentItemsManager,
        onSavePreferences = { savePreferences() },
        onFilterLogs = { windowId -> filterLogs(windowId) }
    )
    
    private val uiToggleIntentHandler = UiToggleIntentHandler(
        state = _state,
        logLoadingCoordinator = logLoadingCoordinator,
        onSavePreferences = { savePreferences() },
        onFilterLogs = { windowId -> filterLogs(windowId) }
    )
    
    private val filterIntentHandler = FilterIntentHandler(
        state = _state,
        onSavePreferences = { savePreferences() },
        onFilterLogs = { windowId -> filterLogs(windowId) }
    )
    
    private val tabWindowIntentHandler = TabWindowIntentHandler(
        state = _state,
        logLoadingCoordinator = logLoadingCoordinator,
        onSavePreferences = { debounce -> savePreferences(debounce = debounce) }
    )
    
    private val entryIntentHandler = EntryIntentHandler(
        state = _state,
        onCopySelectedToClipboard = { copySelectedToClipboard() }
    )
    
    private val dialogIntentHandler = DialogIntentHandler(_state)
    
    private val recentItemsIntentHandler = RecentItemsIntentHandler(
        state = _state,
        recentItemsManager = recentItemsManager,
        onSavePreferences = { savePreferences() }
    )

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
            is KLogViewerIntent.WorkspaceIntent -> workspaceIntentHandler.handle(intent)
            is KLogViewerIntent.UiToggleIntent -> uiToggleIntentHandler.handle(intent)
            is KLogViewerIntent.FilterIntent -> filterIntentHandler.handle(intent)
            is KLogViewerIntent.TabWindowIntent -> tabWindowIntentHandler.handle(intent)
            is KLogViewerIntent.EntryIntent -> entryIntentHandler.handle(intent)
            is KLogViewerIntent.SftpIntent -> sftpIntentHandler.handle(intent)
            is KLogViewerIntent.DialogIntent -> dialogIntentHandler.handle(intent)
            is KLogViewerIntent.RecentItemsIntent -> recentItemsIntentHandler.handle(intent)
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
