package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.AwtClipboard
import com.klogviewer.core.repository.JavaLocalFileSystem
import com.klogviewer.core.analysis.InMemoryAnalysisMetricsRepository
import com.klogviewer.core.source.DefaultLogSourceFactory
import com.klogviewer.core.source.UnifiedRemoteFileSystem
import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.AnalysisFieldKey
import com.klogviewer.domain.model.DiffWindow
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogUpdate
import com.klogviewer.domain.model.TimeBucketSize
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.*
import com.klogviewer.ui.mvi.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}
private const val DASHBOARD_FIELD_QUERY_PREFIX = "@field:"
private const val MISSING_BUCKET_VALUE = "(missing)"

class KLogViewerViewModel(
    private val logSource: LogSource,
    private val prefsRepository: PreferencesRepository,
    val heuristicProbe: HeuristicProbe,
    private val logSourceFactory: LogSourceFactory = DefaultLogSourceFactory(),
    private val clipboard: Clipboard = AwtClipboard(),
    val localFileSystem: LocalFileSystem = JavaLocalFileSystem(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val remoteFileSystem: RemoteFileSystem = UnifiedRemoteFileSystem(),
    private val analysisMetricsRepository: AnalysisMetricsRepository = InMemoryAnalysisMetricsRepository()
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
    private val filterGenerationByWindow = mutableMapOf<String, Long>()

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
        savePreferences(currentState = _state.value, debounce = false)
        logLoadingCoordinator.cancelAll()
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
            is KLogViewerIntent.DashboardIntent -> handleDashboardIntent(intent)
            is KLogViewerIntent.TabWindowIntent -> tabWindowIntentHandler.handle(intent)
            is KLogViewerIntent.EntryIntent -> entryIntentHandler.handle(intent)
            is KLogViewerIntent.SftpIntent -> sftpIntentHandler.handle(intent)
            is KLogViewerIntent.S3Intent -> s3IntentHandler.handle(intent)
            is KLogViewerIntent.DialogIntent -> handleDialogIntent(intent)
            is KLogViewerIntent.RecentItemsIntent -> recentItemsIntentHandler.handle(intent)
            else -> Unit
        }
    }

    private fun handleDialogIntent(intent: KLogViewerIntent.DialogIntent) {
        when (intent) {
            KLogViewerIntent.ConfirmPlaintextSecretSave -> confirmPlaintextSecretSave()
            KLogViewerIntent.DeclinePlaintextSecretSave -> declinePlaintextSecretSave()
            else -> dialogIntentHandler.handle(intent)
        }
    }

    private fun handleDashboardIntent(intent: KLogViewerIntent.DashboardIntent) {
        val activeWindow = _state.value.activeTab?.activeWindow ?: return
        when (intent) {
            KLogViewerIntent.ShowDashboard -> {
                _state.update { state ->
                    state.updateWindow(activeWindow.id) { window ->
                        window.copy(
                            workspaceMode = WorkspaceMode.DASHBOARD,
                            dashboardDataState = DashboardDataState.Loading
                        )
                    }
                }
                filterLogs(activeWindow.id)
            }

            KLogViewerIntent.ShowLogs -> {
                _state.update { state ->
                    state.updateWindow(activeWindow.id) { window ->
                        window.copy(workspaceMode = WorkspaceMode.LOGS)
                    }
                }
            }

            is KLogViewerIntent.SetDashboardBucketSize -> {
                _state.update { state ->
                    state.updateWindow(activeWindow.id) { window ->
                        val nextDashboardState = when (val currentDashboardState = window.dashboardDataState) {
                            is DashboardDataState.Content -> currentDashboardState.copy(
                                bucketSize = intent.bucketSize,
                                selectedBucketFrom = null
                            )

                            else -> DashboardDataState.Loading
                        }
                        window.copy(
                            dashboardBucketSize = intent.bucketSize,
                            dashboardDataState = nextDashboardState
                        )
                    }
                }
                filterLogs(activeWindow.id)
            }

            is KLogViewerIntent.SelectDashboardTimeBucket -> applyDashboardTimeBucketSelection(activeWindow.id, intent.bucketFrom)
            is KLogViewerIntent.SelectDashboardTimeRange -> applyDashboardTimeRangeSelection(
                activeWindow.id,
                intent.from,
                intent.to
            )
            is KLogViewerIntent.SelectDashboardLevel -> applyDashboardLevelSelection(activeWindow.id, intent.level)
            is KLogViewerIntent.SetDashboardFrequencyField -> applyDashboardFrequencyFieldSelection(activeWindow.id, intent.fieldKey)
            is KLogViewerIntent.SetDashboardFrequencyTopN -> applyDashboardFrequencyTopN(activeWindow.id, intent.topN)
            is KLogViewerIntent.SetDashboardFrequencyThreshold -> applyDashboardFrequencyThreshold(activeWindow.id, intent.threshold)
            is KLogViewerIntent.SetDashboardFrequencyCardinalityLimit -> {
                applyDashboardFrequencyCardinalityLimit(activeWindow.id, intent.limit)
            }
            is KLogViewerIntent.SelectDashboardFrequencyValue -> {
                applyDashboardFrequencyValueSelection(activeWindow.id, intent.value)
            }
            is KLogViewerIntent.SetDashboardCompareBaselineFrom -> {
                applyDashboardCompareBaselineFrom(activeWindow.id, intent.from)
            }
            is KLogViewerIntent.SetDashboardCompareBaselineTo -> {
                applyDashboardCompareBaselineTo(activeWindow.id, intent.to)
            }
            is KLogViewerIntent.SetDashboardCompareComparisonFrom -> {
                applyDashboardCompareComparisonFrom(activeWindow.id, intent.from)
            }
            is KLogViewerIntent.SetDashboardCompareComparisonTo -> {
                applyDashboardCompareComparisonTo(activeWindow.id, intent.to)
            }
            KLogViewerIntent.RunDashboardComparison -> filterLogs(activeWindow.id)
            KLogViewerIntent.ClearDashboardComparison -> clearDashboardComparison(activeWindow.id)
            KLogViewerIntent.ClearDashboardSelections -> clearDashboardSelections(activeWindow.id)
        }
    }

    private fun applyDashboardTimeBucketSelection(windowId: String, bucketFrom: Instant) {
        val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return
        val dashboardState = window.dashboardDataState as? DashboardDataState.Content ?: return
        val selectedBucket = dashboardState.timeSeries.find { it.from == bucketFrom } ?: return
        val isClearingSelection = dashboardState.selectedBucketFrom == bucketFrom

        if (isClearingSelection) {
            clearDashboardTimeRangeSelection(windowId)
            return
        }

        updateDashboardTimeRangeSelection(windowId, selectedBucket.from, selectedBucket.to, bucketFrom)
    }

    private fun applyDashboardTimeRangeSelection(windowId: String, from: Instant, to: Instant) {
        if (from.isAfter(to)) return

        val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return
        val dashboardState = window.dashboardDataState as? DashboardDataState.Content ?: return
        val selectedBucketFrom = dashboardState.timeSeries
            .find { it.from == from && it.to == to }
            ?.from
        val isClearingSelection = window.timeFilterFromInstant == from &&
            window.timeFilterToInstant == to &&
            dashboardState.selectedBucketFrom == selectedBucketFrom

        if (isClearingSelection) {
            clearDashboardTimeRangeSelection(windowId)
            return
        }

        updateDashboardTimeRangeSelection(windowId, from, to, selectedBucketFrom)
    }

    private fun updateDashboardTimeRangeSelection(
        windowId: String,
        from: Instant,
        to: Instant,
        selectedBucketFrom: Instant?
    ) {
        val fromValue = from.toString()
        val toValue = to.toString()
        val validationMessage = TimeRangeFilterSupport.validationMessage(fromValue, from, toValue, to)

        _state.update { state ->
            state.updateWindow(windowId) { currentWindow ->
                val currentDashboardData = currentWindow.dashboardDataState as? DashboardDataState.Content
                currentWindow.copy(
                    timeFilterFrom = fromValue,
                    timeFilterTo = toValue,
                    timeFilterFromInstant = from,
                    timeFilterToInstant = to,
                    timeFilterPreset = TimeRangePreset.CUSTOM,
                    timeFilterValidationMessage = validationMessage,
                    dashboardDataState = currentDashboardData?.copy(
                        selectedBucketFrom = selectedBucketFrom
                    ) ?: currentWindow.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun clearDashboardTimeRangeSelection(windowId: String) {
        _state.update { state ->
            state.updateWindow(windowId) { currentWindow ->
                val currentDashboardData = currentWindow.dashboardDataState as? DashboardDataState.Content
                currentWindow.copy(
                    timeFilterFrom = "",
                    timeFilterTo = "",
                    timeFilterFromInstant = null,
                    timeFilterToInstant = null,
                    timeFilterPreset = null,
                    timeFilterValidationMessage = null,
                    dashboardDataState = currentDashboardData?.copy(
                        selectedBucketFrom = null
                    ) ?: currentWindow.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardLevelSelection(windowId: String, level: LogLevel) {
        val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return
        val dashboardState = window.dashboardDataState as? DashboardDataState.Content ?: return
        val isClearingSelection = dashboardState.selectedLevel == level

        _state.update { state ->
            state.updateWindow(windowId) { currentWindow ->
                val allLevels = LogLevel.entries.toSet()
                val currentDashboardData = currentWindow.dashboardDataState as? DashboardDataState.Content
                currentWindow.copy(
                    levelFilters = if (isClearingSelection) allLevels else setOf(level),
                    dashboardDataState = currentDashboardData?.copy(
                        selectedLevel = if (isClearingSelection) null else level
                    ) ?: currentWindow.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardFrequencyFieldSelection(windowId: String, fieldKey: String) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                val updatedDashboardData = currentDashboardData?.copy(
                    selectedFrequencyField = fieldKey,
                    selectedFrequencyValue = null,
                    comparisonState = currentDashboardData.comparisonState.copy(fieldDeltas = emptyList())
                )
                window.copy(
                    filterQueries = removeDashboardFieldFilterQueries(window.filterQueries),
                    dashboardDataState = updatedDashboardData ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardFrequencyTopN(windowId: String, topN: Int) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyTopN = topN.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardFrequencyThreshold(windowId: String, threshold: Int) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyThreshold = threshold.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardFrequencyCardinalityLimit(windowId: String, limit: Int) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyCardinalityLimit = limit.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardFrequencyValueSelection(windowId: String, value: String) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                if (currentDashboardData == null || currentDashboardData.selectedFrequencyField == null) {
                    return@updateWindow window
                }

                val currentSelectedValue = currentDashboardData.selectedFrequencyValue
                val isClearingSelection = currentSelectedValue == value
                val nextSelectedValue = if (isClearingSelection) null else value
                val nextDashboardData = currentDashboardData.copy(selectedFrequencyValue = nextSelectedValue)
                val nextFilterQueries = if (isClearingSelection) {
                    removeDashboardFieldFilterQueries(window.filterQueries)
                } else {
                    removeDashboardFieldFilterQueries(window.filterQueries) + buildDashboardFieldFilterQuery(
                        currentDashboardData.selectedFrequencyField,
                        value
                    )
                }

                window.copy(
                    filterQueries = nextFilterQueries,
                    dashboardDataState = nextDashboardData
                )
            }
        }
        filterLogs(windowId)
    }

    private fun applyDashboardCompareBaselineFrom(windowId: String, from: String) {
        applyDashboardComparisonInputUpdate(windowId, isBaseline = true, fromValue = from)
    }

    private fun applyDashboardCompareBaselineTo(windowId: String, to: String) {
        applyDashboardComparisonInputUpdate(windowId, isBaseline = true, toValue = to)
    }

    private fun applyDashboardCompareComparisonFrom(windowId: String, from: String) {
        applyDashboardComparisonInputUpdate(windowId, isBaseline = false, fromValue = from)
    }

    private fun applyDashboardCompareComparisonTo(windowId: String, to: String) {
        applyDashboardComparisonInputUpdate(windowId, isBaseline = false, toValue = to)
    }

    private fun applyDashboardComparisonInputUpdate(
        windowId: String,
        isBaseline: Boolean,
        fromValue: String? = null,
        toValue: String? = null
    ) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                    ?: return@updateWindow window
                val currentComparisonState = currentDashboardData.comparisonState
                val currentRange = if (isBaseline) currentComparisonState.baselineRange else currentComparisonState.comparisonRange

                val nextFrom = fromValue ?: currentRange.from
                val nextTo = toValue ?: currentRange.to
                val nextFromInstant = TimeRangeFilterSupport.parseInstantOrNull(nextFrom)
                val nextToInstant = TimeRangeFilterSupport.parseInstantOrNull(nextTo)
                val nextValidationMessage = TimeRangeFilterSupport.validationMessage(
                    nextFrom,
                    nextFromInstant,
                    nextTo,
                    nextToInstant
                )

                val nextRange = currentRange.copy(
                    from = nextFrom,
                    to = nextTo,
                    fromInstant = nextFromInstant,
                    toInstant = nextToInstant,
                    validationMessage = nextValidationMessage
                )
                val nextComparisonState = if (isBaseline) {
                    currentComparisonState.copy(
                        baselineRange = nextRange,
                        levelDeltas = emptyList(),
                        fieldDeltas = emptyList()
                    )
                } else {
                    currentComparisonState.copy(
                        comparisonRange = nextRange,
                        levelDeltas = emptyList(),
                        fieldDeltas = emptyList()
                    )
                }

                window.copy(
                    dashboardDataState = currentDashboardData.copy(comparisonState = nextComparisonState)
                )
            }
        }
        invalidatePendingFilterResults(windowId)
    }

    private fun clearDashboardComparison(windowId: String) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        comparisonState = DashboardComparisonState()
                    ) ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
    }

    private fun removeDashboardFieldFilterQueries(filterQueries: List<String>): List<String> {
        return filterQueries.filterNot { it.startsWith(DASHBOARD_FIELD_QUERY_PREFIX) }
    }

    private fun buildDashboardFieldFilterQuery(fieldKey: String, value: String): String {
        return "$DASHBOARD_FIELD_QUERY_PREFIX$fieldKey=$value"
    }

    private fun invalidatePendingFilterResults(windowId: String) {
        synchronized(filterGenerationByWindow) {
            val next = (filterGenerationByWindow[windowId] ?: 0L) + 1L
            filterGenerationByWindow[windowId] = next
        }
    }

    private fun clearDashboardSelections(windowId: String) {
        _state.update { state ->
            state.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    filterQueries = removeDashboardFieldFilterQueries(window.filterQueries),
                    levelFilters = LogLevel.entries.toSet(),
                    timeFilterFrom = "",
                    timeFilterTo = "",
                    timeFilterFromInstant = null,
                    timeFilterToInstant = null,
                    timeFilterPreset = null,
                    timeFilterValidationMessage = null,
                    dashboardDataState = currentDashboardData?.copy(
                        selectedBucketFrom = null,
                        selectedLevel = null,
                        selectedFrequencyValue = null,
                        comparisonState = DashboardComparisonState()
                    ) ?: window.dashboardDataState
                )
            }
        }
        filterLogs(windowId)
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

        val generation = synchronized(filterGenerationByWindow) {
            val next = (filterGenerationByWindow[windowId] ?: 0L) + 1L
            filterGenerationByWindow[windowId] = next
            next
        }
        
        scope.launch(Dispatchers.Default) {
            val window = _state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return@launch
            val filteredLogs = LogFilterService.filter(window)
            val dashboardDataState = buildDashboardDataState(
                filteredLogs = filteredLogs,
                bucketSize = window.dashboardBucketSize,
                previousState = window.dashboardDataState
            )

            val isLatestGeneration = synchronized(filterGenerationByWindow) {
                filterGenerationByWindow[windowId] == generation
            }
            if (!isLatestGeneration) {
                return@launch
            }
            
            _state.update { currentState ->
                currentState.updateWindow(windowId) {
                    it.copy(
                        filteredLogs = filteredLogs,
                        dashboardDataState = dashboardDataState
                    )
                }
            }
        }
    }

    private suspend fun buildDashboardDataState(
        filteredLogs: List<LogEntry>,
        bucketSize: DashboardBucketSize,
        previousState: DashboardDataState
    ): DashboardDataState {
        if (filteredLogs.isEmpty()) return DashboardDataState.Empty

        val previousContent = previousState as? DashboardDataState.Content
        val availableFrequencyFields = filteredLogs.asSequence()
            .flatMap { entry -> entry.fields.keys.asSequence() }
            .distinct()
            .sorted()
            .toList()
        val selectedFrequencyField = previousContent?.selectedFrequencyField
            ?.takeIf { field -> availableFrequencyFields.contains(field) }
            ?: availableFrequencyFields.firstOrNull()
        val frequencyTopN = (previousContent?.frequencyTopN ?: 10).coerceAtLeast(1)
        val frequencyThreshold = (previousContent?.frequencyThreshold ?: 1).coerceAtLeast(1)
        val frequencyCardinalityLimit = (previousContent?.frequencyCardinalityLimit ?: 100).coerceAtLeast(1)
        val frequencyItems = computeFrequencyItems(
            entries = filteredLogs,
            selectedField = selectedFrequencyField,
            cardinalityLimit = frequencyCardinalityLimit,
            threshold = frequencyThreshold,
            topN = frequencyTopN
        )
        val selectedFrequencyValue = previousContent?.selectedFrequencyValue
            ?.takeIf { selectedValue -> frequencyItems.any { it.value == selectedValue } }
        val comparisonState = computeComparisonState(
            entries = filteredLogs,
            previousState = previousContent?.comparisonState ?: DashboardComparisonState(),
            selectedField = selectedFrequencyField,
            threshold = frequencyThreshold,
            topN = frequencyTopN,
            cardinalityLimit = frequencyCardinalityLimit
        )

        val timeSeriesResult = analysisMetricsRepository.timeSeriesMetrics(
            TimeSeriesMetricsQuery(
                entries = filteredLogs,
                bucketSize = bucketSize.toDomainBucketSize(),
                window = DiffWindow.Unbounded
            )
        )

        return timeSeriesResult.fold(
            ifLeft = { failure ->
                DashboardDataState.Error(failure.toDashboardErrorMessage())
            },
            ifRight = { result ->
                val timeSeries = result.buckets
                    .asSequence()
                    .sortedBy { bucket -> bucket.window.from }
                    .map { bucket ->
                        DashboardTimeBucket(
                            from = bucket.window.from,
                            to = bucket.window.to,
                            count = bucket.count.value
                        )
                    }
                    .toList()
                val levelDistribution = computeNormalizedLevelDistribution(filteredLogs)
                val selectedBucketFrom = previousContent?.selectedBucketFrom
                    ?.takeIf { selectedFrom -> timeSeries.any { it.from == selectedFrom } }
                val selectedLevel = previousContent?.selectedLevel
                    ?.takeIf { selected -> levelDistribution.any { it.level == selected && it.count > 0 } }

                DashboardDataState.Content(
                    bucketSize = bucketSize,
                    totalEvents = filteredLogs.size,
                    timeSeries = timeSeries,
                    levelDistribution = levelDistribution,
                    availableFrequencyFields = availableFrequencyFields,
                    selectedFrequencyField = selectedFrequencyField,
                    frequencyTopN = frequencyTopN,
                    frequencyThreshold = frequencyThreshold,
                    frequencyCardinalityLimit = frequencyCardinalityLimit,
                    frequencyItems = frequencyItems,
                    selectedBucketFrom = selectedBucketFrom,
                    selectedLevel = selectedLevel,
                    selectedFrequencyValue = selectedFrequencyValue,
                    comparisonState = comparisonState
                )
            }
        )
    }

    private suspend fun computeFrequencyItems(
        entries: List<LogEntry>,
        selectedField: String?,
        cardinalityLimit: Int,
        threshold: Int,
        topN: Int
    ): List<DashboardFieldFrequencyItem> {
        val fieldKey = selectedField ?: return emptyList()

        return AnalysisFieldKey.from(fieldKey).fold(
            ifLeft = { emptyList() },
            ifRight = { validFieldKey ->
                analysisMetricsRepository.frequencyAnalysis(
                    FieldFrequencyQuery(
                        entries = entries,
                        fieldKey = validFieldKey,
                        limit = cardinalityLimit,
                        window = DiffWindow.Unbounded
                    )
                ).fold(
                    ifLeft = { emptyList() },
                    ifRight = { result ->
                        result.frequencies
                            .asSequence()
                            .sortedWith(compareByDescending<com.klogviewer.domain.model.FieldFrequencyItem> { it.count.value }.thenBy { it.value })
                            .filter { item -> item.count.value >= threshold }
                            .take(topN)
                            .map { item ->
                                DashboardFieldFrequencyItem(
                                    value = item.value,
                                    count = item.count.value
                                )
                            }
                            .toList()
                    }
                )
            }
        )
    }

    private fun computeComparisonState(
        entries: List<LogEntry>,
        previousState: DashboardComparisonState,
        selectedField: String?,
        threshold: Int,
        topN: Int,
        cardinalityLimit: Int
    ): DashboardComparisonState {
        val baselineRange = previousState.baselineRange
        val comparisonRange = previousState.comparisonRange
        val hasBaselineInput = baselineRange.from.isNotBlank() || baselineRange.to.isNotBlank()
        val hasComparisonInput = comparisonRange.from.isNotBlank() || comparisonRange.to.isNotBlank()

        if (!hasBaselineInput || !hasComparisonInput) {
            return previousState.copy(levelDeltas = emptyList(), fieldDeltas = emptyList())
        }
        if (baselineRange.validationMessage != null || comparisonRange.validationMessage != null) {
            return previousState.copy(levelDeltas = emptyList(), fieldDeltas = emptyList())
        }

        val baselineWindow = DiffWindow(from = baselineRange.fromInstant, to = baselineRange.toInstant)
        val comparisonWindow = DiffWindow(from = comparisonRange.fromInstant, to = comparisonRange.toInstant)
        val baselineEntries = entries.filterInWindow(baselineWindow)
        val comparisonEntries = entries.filterInWindow(comparisonWindow)

        val levelDeltas = LogLevel.entries.map { level ->
            val baselineCount = baselineEntries.count { it.level == level }
            val comparisonCount = comparisonEntries.count { it.level == level }
            val delta = comparisonCount - baselineCount

            DashboardLevelDelta(
                level = level,
                baselineCount = baselineCount,
                comparisonCount = comparisonCount,
                delta = delta,
                direction = deltaDirection(delta)
            )
        }

        val fieldDeltas = selectedField?.let { fieldKey ->
            val baselineCounts = baselineEntries.countFieldValues(fieldKey)
            val comparisonCounts = comparisonEntries.countFieldValues(fieldKey)
            (baselineCounts.keys + comparisonCounts.keys)
                .toSortedSet()
                .map { value ->
                    val baselineCount = baselineCounts[value] ?: 0
                    val comparisonCount = comparisonCounts[value] ?: 0
                    val delta = comparisonCount - baselineCount

                    DashboardFieldFrequencyItem(
                        value = value,
                        count = comparisonCount,
                        delta = delta,
                        direction = deltaDirection(delta)
                    )
                }
                .filter { item ->
                    val baselineCount = baselineCounts[item.value] ?: 0
                    maxOf(baselineCount, item.count) >= threshold
                }
                .sortedWith(
                    compareByDescending<DashboardFieldFrequencyItem> { abs(it.delta ?: 0) }
                        .thenByDescending { it.count }
                        .thenBy { it.value }
                )
                .take(cardinalityLimit)
                .take(topN)
        } ?: emptyList()

        return previousState.copy(
            levelDeltas = levelDeltas,
            fieldDeltas = fieldDeltas
        )
    }

    private fun List<LogEntry>.filterInWindow(window: DiffWindow): List<LogEntry> {
        return filter { entry ->
            val instant = entry.instant ?: return@filter false
            window.contains(instant)
        }
    }

    private fun List<LogEntry>.countFieldValues(fieldKey: String): Map<String, Int> {
        return groupingBy { entry -> entry.fields[fieldKey] ?: MISSING_BUCKET_VALUE }
            .eachCount()
    }

    private fun deltaDirection(delta: Int): DashboardDeltaDirection {
        return when {
            delta > 0 -> DashboardDeltaDirection.INCREASE
            delta < 0 -> DashboardDeltaDirection.DECREASE
            else -> DashboardDeltaDirection.UNCHANGED
        }
    }

    private fun computeNormalizedLevelDistribution(entries: List<LogEntry>): List<DashboardLevelSlice> {
        if (entries.isEmpty()) {
            return LogLevel.entries.map { level -> DashboardLevelSlice(level = level, count = 0, ratio = 0f) }
        }

        val countsByLevel = entries.groupingBy { it.level }.eachCount()
        val totalEntries = entries.size.toFloat()

        return LogLevel.entries.map { level ->
            val count = countsByLevel[level] ?: 0
            DashboardLevelSlice(
                level = level,
                count = count,
                ratio = count / totalEntries
            )
        }
    }

    private fun DashboardBucketSize.toDomainBucketSize(): TimeBucketSize {
        return when (this) {
            DashboardBucketSize.PER_SECOND -> TimeBucketSize.ONE_SECOND
            DashboardBucketSize.PER_MINUTE -> TimeBucketSize.ONE_MINUTE
        }
    }

    private fun AnalysisFailure.toDashboardErrorMessage(): String {
        return when (this) {
            AnalysisFailure.NoTimestampData -> "Dashboard requires timestamped logs"
            is AnalysisFailure.InvalidTimeBucketSize -> "Invalid dashboard bucket size"
            is AnalysisFailure.InvalidDiffWindow -> "Invalid dashboard time range"
            else -> "Dashboard analysis failed"
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
