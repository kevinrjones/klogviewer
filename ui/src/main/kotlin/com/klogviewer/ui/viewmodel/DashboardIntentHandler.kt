package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.AnalysisFailure
import com.klogviewer.domain.model.AnalysisFieldKey
import com.klogviewer.domain.model.DiffWindow
import com.klogviewer.domain.model.FieldFrequencyQuery
import com.klogviewer.domain.model.LevelFilterKey
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.TimeBucketSize
import com.klogviewer.domain.model.TimeSeriesMetricsQuery
import com.klogviewer.domain.repository.AnalysisMetricsRepository
import com.klogviewer.ui.mvi.DashboardBucketSize
import com.klogviewer.ui.mvi.DashboardComparisonState
import com.klogviewer.ui.mvi.DashboardDataState
import com.klogviewer.ui.mvi.DashboardDeltaDirection
import com.klogviewer.ui.mvi.DashboardFieldFrequencyItem
import com.klogviewer.ui.mvi.DashboardLevelDelta
import com.klogviewer.ui.mvi.DashboardLevelSlice
import com.klogviewer.ui.mvi.DashboardSamplingInfo
import com.klogviewer.ui.mvi.DashboardSamplingMode
import com.klogviewer.ui.mvi.DashboardTimeBucket
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TimeRangePreset
import com.klogviewer.ui.mvi.WorkspaceMode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import kotlin.math.abs

private val dashboardLogger = KotlinLogging.logger {}

class DashboardIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val analysisMetricsRepository: AnalysisMetricsRepository,
    private val dashboardSamplingThreshold: Int,
    private val dashboardSamplingTargetSize: Int,
    private val onFilterLogs: (String) -> Unit,
    private val onSavePreferences: (Boolean) -> Unit
) {
    private val filterRecomputeLock = Any()
    private val filterGenerationByWindow = mutableMapOf<String, Long>()
    private val filterRecomputeJobByWindow = mutableMapOf<String, Job>()

    fun handle(intent: KLogViewerIntent.DashboardIntent) {
        val activeWindowId = state.value.activeTab?.activeWindow?.id ?: return
        when (intent) {
            KLogViewerIntent.ShowDashboard -> {
                state.update { s ->
                    s.updateWindow(activeWindowId) { window ->
                        window.copy(
                            workspaceMode = WorkspaceMode.DASHBOARD,
                            dashboardDataState = DashboardDataState.Loading
                        )
                    }
                }
                onFilterLogs(activeWindowId)
            }

            KLogViewerIntent.ShowLogs -> {
                state.update { s ->
                    s.updateWindow(activeWindowId) { window ->
                        window.copy(workspaceMode = WorkspaceMode.LOGS)
                    }
                }
            }

            is KLogViewerIntent.SetDashboardBucketSize -> {
                state.update { s ->
                    s.updateWindow(activeWindowId) { window ->
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
                onFilterLogs(activeWindowId)
            }

            is KLogViewerIntent.SelectDashboardTimeBucket -> applyDashboardTimeBucketSelection(activeWindowId, intent.bucketFrom)
            is KLogViewerIntent.SelectDashboardTimeRange -> applyDashboardTimeRangeSelection(
                activeWindowId,
                intent.from,
                intent.to
            )
            is KLogViewerIntent.SelectDashboardLevel -> applyDashboardLevelSelection(activeWindowId, intent.level)
            is KLogViewerIntent.SetDashboardFrequencyField -> applyDashboardFrequencyFieldSelection(activeWindowId, intent.fieldKey)
            is KLogViewerIntent.SetDashboardFrequencyTopN -> applyDashboardFrequencyTopN(activeWindowId, intent.topN)
            is KLogViewerIntent.SetDashboardFrequencyThreshold -> applyDashboardFrequencyThreshold(activeWindowId, intent.threshold)
            is KLogViewerIntent.SetDashboardFrequencyCardinalityLimit -> {
                applyDashboardFrequencyCardinalityLimit(activeWindowId, intent.limit)
            }
            is KLogViewerIntent.SelectDashboardFrequencyValue -> {
                applyDashboardFrequencyValueSelection(activeWindowId, intent.value)
            }
            is KLogViewerIntent.SetDashboardCompareBaselineFrom -> {
                applyDashboardCompareBaselineFrom(activeWindowId, intent.from)
            }
            is KLogViewerIntent.SetDashboardCompareBaselineTo -> {
                applyDashboardCompareBaselineTo(activeWindowId, intent.to)
            }
            is KLogViewerIntent.SetDashboardCompareComparisonFrom -> {
                applyDashboardCompareComparisonFrom(activeWindowId, intent.from)
            }
            is KLogViewerIntent.SetDashboardCompareComparisonTo -> {
                applyDashboardCompareComparisonTo(activeWindowId, intent.to)
            }
            KLogViewerIntent.RunDashboardComparison -> onFilterLogs(activeWindowId)
            KLogViewerIntent.ClearDashboardComparison -> clearDashboardComparison(activeWindowId)
            KLogViewerIntent.ClearDashboardSelections -> clearDashboardSelections(activeWindowId)
        }
    }

    private fun applyDashboardTimeBucketSelection(windowId: String, bucketFrom: Instant) {
        val window = state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return
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

        val window = state.value.tabs.flatMap { it.windows }.find { it.id == windowId } ?: return
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

        state.update { s ->
            s.updateWindow(windowId) { currentWindow ->
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
        onSavePreferences(false)
        onFilterLogs(windowId)
    }

    private fun clearDashboardTimeRangeSelection(windowId: String) {
        state.update { s ->
            s.updateWindow(windowId) { currentWindow ->
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
        onSavePreferences(false)
        onFilterLogs(windowId)
    }

    private fun applyDashboardLevelSelection(windowId: String, level: LogLevel) {
        state.update { s ->
            s.updateWindow(windowId) { currentWindow ->
                val currentDashboardData = currentWindow.dashboardDataState as? DashboardDataState.Content
                    ?: return@updateWindow currentWindow
                val isClearingSelection = currentDashboardData.selectedLevel == level
                val allLevels = currentWindow.availableLevels.toSet().ifEmpty { LevelFilterPolicy.defaultFilters }
                currentWindow.copy(
                    levelFilters = if (isClearingSelection) {
                        allLevels
                    } else {
                        setOf(LevelFilterKey.fromLogLevel(level))
                    },
                    dashboardDataState = currentDashboardData.copy(
                        selectedLevel = if (isClearingSelection) null else level
                    )
                )
            }
        }
        onFilterLogs(windowId)
    }

    private fun applyDashboardFrequencyFieldSelection(windowId: String, fieldKey: String) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
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
        onFilterLogs(windowId)
    }

    private fun applyDashboardFrequencyTopN(windowId: String, topN: Int) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyTopN = topN.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        onFilterLogs(windowId)
    }

    private fun applyDashboardFrequencyThreshold(windowId: String, threshold: Int) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyThreshold = threshold.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        onFilterLogs(windowId)
    }

    private fun applyDashboardFrequencyCardinalityLimit(windowId: String, limit: Int) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        frequencyCardinalityLimit = limit.coerceAtLeast(1)
                    ) ?: window.dashboardDataState
                )
            }
        }
        onFilterLogs(windowId)
    }

    private fun applyDashboardFrequencyValueSelection(windowId: String, value: String) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
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
        onFilterLogs(windowId)
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
        state.update { s ->
            s.updateWindow(windowId) { window ->
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
        state.update { s ->
            s.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    dashboardDataState = currentDashboardData?.copy(
                        comparisonState = DashboardComparisonState()
                    ) ?: window.dashboardDataState
                )
            }
        }
        onFilterLogs(windowId)
    }

    private fun removeDashboardFieldFilterQueries(filterQueries: List<String>): List<String> {
        return filterQueries.filterNot { it.startsWith(DASHBOARD_FIELD_QUERY_PREFIX) }
    }

    private fun buildDashboardFieldFilterQuery(fieldKey: String, value: String): String {
        return "$DASHBOARD_FIELD_QUERY_PREFIX$fieldKey=$value"
    }

    private fun invalidatePendingFilterResults(windowId: String) {
        val previousJob = synchronized(filterRecomputeLock) {
            val next = (filterGenerationByWindow[windowId] ?: 0L) + 1L
            filterGenerationByWindow[windowId] = next
            filterRecomputeJobByWindow.remove(windowId)
        }
        previousJob?.cancel()
    }

    private fun clearDashboardSelections(windowId: String) {
        state.update { s ->
            s.updateWindow(windowId) { window ->
                val currentDashboardData = window.dashboardDataState as? DashboardDataState.Content
                window.copy(
                    filterQueries = removeDashboardFieldFilterQueries(window.filterQueries),
                    levelFilters = window.availableLevels.toSet().ifEmpty { LevelFilterPolicy.defaultFilters },
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
        onFilterLogs(windowId)
    }

    suspend fun buildDashboardDataState(
        filteredLogs: List<LogEntry>,
        bucketSize: DashboardBucketSize,
        previousState: DashboardDataState
    ): DashboardDataState {
        if (filteredLogs.isEmpty()) return DashboardDataState.Empty

        val sampledEntries = deterministicSample(
            entries = filteredLogs,
            threshold = dashboardSamplingThreshold,
            targetSize = dashboardSamplingTargetSize
        )
        val previousContent = previousState as? DashboardDataState.Content

        val frequencyConfig = resolveFrequencyConfig(previousContent, sampledEntries)
        val frequencyItems = computeFrequencyItems(
            entries = sampledEntries.entries,
            selectedField = frequencyConfig.selectedFrequencyField,
            cardinalityLimit = frequencyConfig.cardinalityLimit,
            threshold = frequencyConfig.threshold,
            topN = frequencyConfig.topN
        )
        val selectedFrequencyValue = previousContent?.selectedFrequencyValue
            ?.takeIf { selectedValue -> frequencyItems.any { it.value == selectedValue } }
        val comparisonState = computeComparisonState(
            entries = sampledEntries.entries,
            previousState = previousContent?.comparisonState ?: DashboardComparisonState(),
            selectedField = frequencyConfig.selectedFrequencyField,
            threshold = frequencyConfig.threshold,
            topN = frequencyConfig.topN,
            cardinalityLimit = frequencyConfig.cardinalityLimit
        )

        return buildContentState(
            sampledEntries = sampledEntries,
            filteredLogs = filteredLogs,
            bucketSize = bucketSize,
            previousContent = previousContent,
            frequencyConfig = frequencyConfig,
            frequencyItems = frequencyItems,
            selectedFrequencyValue = selectedFrequencyValue,
            comparisonState = comparisonState
        )
    }

    private fun resolveFrequencyConfig(
        previousContent: DashboardDataState.Content?,
        sampledEntries: SampledEntries
    ): FrequencyConfig {
        val discoveredFrequencyFields = collectAvailableFrequencyFields(
            entries = sampledEntries.entries,
            discoveredFieldLimit = DASHBOARD_STRUCTURED_DISCOVERED_FIELD_LIMIT
        )
        val availableFrequencyFields = previousContent?.selectedFrequencyField
            ?.takeIf { it.isNotBlank() }
            ?.let { selectedField ->
                if (discoveredFrequencyFields.contains(selectedField)) {
                    discoveredFrequencyFields
                } else {
                    (discoveredFrequencyFields + selectedField).sorted()
                }
            }
            ?: discoveredFrequencyFields
        val selectedField = previousContent?.selectedFrequencyField
            ?.takeIf { field -> availableFrequencyFields.contains(field) }
            ?: availableFrequencyFields.firstOrNull()
        return FrequencyConfig(
            selectedFrequencyField = selectedField,
            topN = (previousContent?.frequencyTopN ?: 10).coerceAtLeast(1),
            threshold = (previousContent?.frequencyThreshold ?: 1).coerceAtLeast(1),
            cardinalityLimit = (previousContent?.frequencyCardinalityLimit ?: 100).coerceAtLeast(1),
            availableFrequencyFields = availableFrequencyFields
        )
    }

    private suspend fun buildContentState(
        sampledEntries: SampledEntries,
        filteredLogs: List<LogEntry>,
        bucketSize: DashboardBucketSize,
        previousContent: DashboardDataState.Content?,
        frequencyConfig: FrequencyConfig,
        frequencyItems: List<DashboardFieldFrequencyItem>,
        selectedFrequencyValue: String?,
        comparisonState: DashboardComparisonState
    ): DashboardDataState {
        val timeSeriesResult = analysisMetricsRepository.timeSeriesMetrics(
            TimeSeriesMetricsQuery(
                entries = sampledEntries.entries,
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
                val levelDistribution = computeNormalizedLevelDistribution(sampledEntries.entries)
                val selectedBucketFrom = previousContent?.selectedBucketFrom
                    ?.takeIf { selectedFrom -> timeSeries.any { it.from == selectedFrom } }
                val selectedLevel = previousContent?.selectedLevel
                    ?.takeIf { selected -> levelDistribution.any { it.level == selected && it.count > 0 } }

                DashboardDataState.Content(
                    bucketSize = bucketSize,
                    totalEvents = filteredLogs.size,
                    timeSeries = timeSeries,
                    levelDistribution = levelDistribution,
                    availableFrequencyFields = frequencyConfig.availableFrequencyFields,
                    selectedFrequencyField = frequencyConfig.selectedFrequencyField,
                    frequencyTopN = frequencyConfig.topN,
                    frequencyThreshold = frequencyConfig.threshold,
                    frequencyCardinalityLimit = frequencyConfig.cardinalityLimit,
                    frequencyItems = frequencyItems,
                    selectedBucketFrom = selectedBucketFrom,
                    selectedLevel = selectedLevel,
                    selectedFrequencyValue = selectedFrequencyValue,
                    comparisonState = comparisonState,
                    samplingInfo = DashboardSamplingInfo(
                        originalCount = sampledEntries.originalCount,
                        sampledCount = sampledEntries.entries.size,
                        mode = sampledEntries.mode
                    ),
                    aggregationCompletedAtEpochMillis = System.currentTimeMillis()
                )
            }
        )
    }

    private data class FrequencyConfig(
        val selectedFrequencyField: String?,
        val topN: Int,
        val threshold: Int,
        val cardinalityLimit: Int,
        val availableFrequencyFields: List<String>
    )

    fun mergeDashboardSelectionsWithLatestState(
        computedState: DashboardDataState,
        latestWindow: LogWindow
    ): DashboardDataState {
        val computedContent = computedState as? DashboardDataState.Content ?: return computedState
        val latestContent = latestWindow.dashboardDataState as? DashboardDataState.Content ?: return computedState

        val selectedBucketFrom = latestContent.selectedBucketFrom
            ?.takeIf { selectedFrom -> computedContent.timeSeries.any { bucket -> bucket.from == selectedFrom } }
        val selectedLevel = latestContent.selectedLevel
            ?.takeIf { selected -> latestWindow.levelFilters == setOf(LevelFilterKey.fromLogLevel(selected)) }
            ?.takeIf { selected -> computedContent.levelDistribution.any { distribution -> distribution.level == selected && distribution.count > 0 } }
        val selectedFrequencyValue = latestContent.selectedFrequencyValue
            ?.takeIf { selectedValue -> computedContent.frequencyItems.any { item -> item.value == selectedValue } }

        return computedContent.copy(
            selectedBucketFrom = selectedBucketFrom,
            selectedLevel = selectedLevel,
            selectedFrequencyValue = selectedFrequencyValue
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
                        val filteredItems = result.frequencies
                            .asSequence()
                            .sortedWith(compareByDescending<com.klogviewer.domain.model.FieldFrequencyItem> { it.count.value }.thenBy { it.value })
                            .filter { item -> item.count.value >= threshold }
                            .toList()
                        val retainedItems = filteredItems
                            .take(topN)
                            .map { item ->
                                DashboardFieldFrequencyItem(
                                    value = item.value,
                                    count = item.count.value
                                )
                            }
                        val overflowCount = filteredItems.drop(topN).sumOf { item -> item.count.value }

                        if (overflowCount > 0) {
                            retainedItems + DashboardFieldFrequencyItem(
                                value = OTHER_BUCKET_VALUE,
                                count = overflowCount
                            )
                        } else {
                            retainedItems
                        }
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
        return groupingBy { entry -> entry.resolveDashboardFieldValue(fieldKey) }
            .eachCount()
    }

    private fun collectAvailableFrequencyFields(
        entries: List<LogEntry>,
        discoveredFieldLimit: Int
    ): List<String> {
        val canonicalFields = entries.asSequence()
            .flatMap { entry -> entry.fields.keys.asSequence() }
            .distinct()
            .sorted()
            .toList()

        val canonicalFieldSet = canonicalFields.toSet()
        val structuredFields = entries.asSequence()
            .flatMap { entry -> entry.structuredData?.toCompatibilityFields()?.keys?.asSequence() ?: emptySequence() }
            .filterNot { field -> canonicalFieldSet.contains(field) }
            .distinct()
            .sorted()
            .take(discoveredFieldLimit)
            .toList()

        return canonicalFields + structuredFields
    }

    private fun LogEntry.resolveDashboardFieldValue(fieldKey: String): String {
        val explicitValue = fields[fieldKey]?.takeIf { it.isNotBlank() }
        if (explicitValue != null) {
            return explicitValue
        }

        val structuredValue = structuredData?.toCompatibilityFields()?.get(fieldKey)
            ?.takeIf { value -> value.isNotBlank() }
        return structuredValue ?: MISSING_BUCKET_VALUE
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

    private fun deterministicSample(
        entries: List<LogEntry>,
        threshold: Int,
        targetSize: Int
    ): SampledEntries {
        if (entries.size <= threshold || targetSize <= 0 || entries.size <= targetSize) {
            return SampledEntries(
                entries = entries,
                originalCount = entries.size,
                mode = DashboardSamplingMode.FULL
            )
        }

        val step = entries.size.toDouble() / targetSize.toDouble()
        val sampled = List(targetSize) { index ->
            val sourceIndex = (index * step).toInt().coerceIn(0, entries.lastIndex)
            entries[sourceIndex]
        }

        dashboardLogger.info {
            "Applied deterministic sampling originalCount=${entries.size} sampledCount=${sampled.size} step=${"%.3f".format(step)}"
        }

        return SampledEntries(
            entries = sampled,
            originalCount = entries.size,
            mode = DashboardSamplingMode.DETERMINISTIC
        )
    }

    private data class SampledEntries(
        val entries: List<LogEntry>,
        val originalCount: Int,
        val mode: DashboardSamplingMode
    )

    private fun nanosToMillis(nanos: Long): Long = nanos / 1_000_000

    companion object {
        private const val DASHBOARD_FIELD_QUERY_PREFIX = "@field:"
        private const val MISSING_BUCKET_VALUE = "(missing)"
        private const val OTHER_BUCKET_VALUE = "(other)"
        private const val DASHBOARD_STRUCTURED_DISCOVERED_FIELD_LIMIT = 200
    }
}
