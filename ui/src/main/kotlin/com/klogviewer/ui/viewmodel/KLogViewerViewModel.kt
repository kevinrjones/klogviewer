package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.AwtClipboard
import com.klogviewer.core.repository.JavaLocalFileSystem
import com.klogviewer.core.source.DefaultLogSourceFactory
import com.klogviewer.core.source.UnifiedRemoteFileSystem
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.*
import com.klogviewer.ui.mvi.KLogViewerEvent
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.PlaintextSecretSavePrompt
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val remoteFileSystem: RemoteFileSystem = UnifiedRemoteFileSystem()
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
    private var pendingPreferencesForPlaintextSecretSave: UserPreferences? = null

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
        recentItemsManager = recentItemsManager,
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

    private val s3IntentHandler = S3IntentHandler(
        remoteFileSystem = remoteFileSystem,
        scope = scope,
        state = _state,
        recentItemsManager = recentItemsManager,
        onSavePreferences = { savePreferences() },
        onLoadFiles = { windowId, paths -> logLoadingCoordinator.loadFilesIntoWindow(windowId, paths) },
        onConnectS3 = { windowId, config ->
            logLoadingCoordinator.connectS3(windowId, config)
        },
        onConnectMultipleS3 = { windowId, config, keys ->
            logLoadingCoordinator.connectMultipleS3(windowId, config, keys)
        },
        onConnectS3Directory = { windowId, config, prefix ->
            logLoadingCoordinator.connectS3Directory(windowId, config, prefix)
        },
        onHandleBrowse = { config, prefix ->
            scope.launch {
                _state.update { it.copy(isRemoteLoading = true, pendingDialog = KLogViewerState.DialogType.S3_BROWSE, currentS3Config = config) }
                val result = remoteFileSystem.listS3Objects(config, prefix)
                _state.update {
                    it.copy(
                        isRemoteLoading = false,
                        remoteFiles = result.getOrNull() ?: emptyList(),
                        remoteBrowsePath = prefix
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
            is KLogViewerIntent.S3Intent -> s3IntentHandler.handle(intent)
            is KLogViewerIntent.DialogIntent -> handleDialogIntent(intent)
            is KLogViewerIntent.RecentItemsIntent -> recentItemsIntentHandler.handle(intent)
        }
    }

    private fun handleDialogIntent(intent: KLogViewerIntent.DialogIntent) {
        when (intent) {
            KLogViewerIntent.ConfirmPlaintextSecretSave -> confirmPlaintextSecretSave()
            KLogViewerIntent.DeclinePlaintextSecretSave -> declinePlaintextSecretSave()
            else -> dialogIntentHandler.handle(intent)
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


    fun savePreferences(
        currentState: KLogViewerState = _state.value,
        debounce: Boolean = false,
        allowPlaintextSecretFallback: Boolean = false
    ) {
        if (debounce) {
            saveJob?.cancel()
            saveJob = scope.launch {
                delay(500.milliseconds)
                performSave(currentState, allowPlaintextSecretFallback)
            }
        } else {
            saveJob?.cancel()
            performSave(currentState, allowPlaintextSecretFallback)
        }
    }

    private fun performSave(
        currentState: KLogViewerState,
        allowPlaintextSecretFallback: Boolean
    ) {
        val currentPrefs = prefsRepository.load()
        val newPrefs = PreferencesStateMapper.toPreferences(currentState, currentPrefs)
        savePreferencesInternal(newPrefs, allowPlaintextSecretFallback)
    }

    private fun savePreferencesInternal(
        preferences: UserPreferences,
        allowPlaintextSecretFallback: Boolean
    ) {
        when (
            val saveResult = prefsRepository.save(
                preferences = preferences,
                options = PreferencesSaveOptions(
                    allowPlaintextSecretFallback = allowPlaintextSecretFallback
                )
            )
        ) {
            PreferencesSaveResult.Saved -> {
                pendingPreferencesForPlaintextSecretSave = null
                _state.update { it.copy(pendingPlaintextSecretSave = null) }
            }

            PreferencesSaveResult.RequiresPlaintextSecretConfirmation -> {
                pendingPreferencesForPlaintextSecretSave = preferences
                _state.update {
                    it.copy(
                        pendingPlaintextSecretSave = PlaintextSecretSavePrompt(
                            title = "Secure storage unavailable",
                            message = "KLogViewer could not access the OS secure credential store. Do you want to save this secret in plaintext in preferences.json?"
                        )
                    )
                }
            }

            is PreferencesSaveResult.Failed -> {
                pendingPreferencesForPlaintextSecretSave = null
                _state.update { it.copy(pendingPlaintextSecretSave = null) }
                scope.launch {
                    _events.emit(
                        KLogViewerEvent.ShowError(
                            saveResult.reason ?: "Failed to save preferences"
                        )
                    )
                }
            }
        }
    }

    private fun confirmPlaintextSecretSave() {
        val pendingPreferences = pendingPreferencesForPlaintextSecretSave ?: return
        savePreferencesInternal(
            preferences = pendingPreferences,
            allowPlaintextSecretFallback = true
        )
    }

    private fun declinePlaintextSecretSave() {
        pendingPreferencesForPlaintextSecretSave = null
        _state.update { it.copy(pendingPlaintextSecretSave = null) }
        scope.launch {
            _events.emit(
                KLogViewerEvent.ShowError(
                    "Preferences with remote credentials were not saved because plaintext fallback was declined"
                )
            )
        }
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
