package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.DefaultPatternPreviewService
import com.klogviewer.core.parser.PatternPreviewService
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PatternWizardIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>,
    private val patternPreviewService: PatternPreviewService = DefaultPatternPreviewService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onResampleLines: ((String) -> List<String>)? = null,
    private val onApplyDraft: ((windowId: String, draft: PatternDraft) -> Unit)? = null
) {
    private var previewJob: Job? = null
    private var previewGeneration = 0L

    fun handle(intent: KLogViewerIntent.PatternWizardIntent) {
        when (intent) {
            is KLogViewerIntent.OpenPatternWizard -> handleOpen(intent)
            KLogViewerIntent.ClosePatternWizard,
            KLogViewerIntent.SkipPatternWizard -> handleClose()
            KLogViewerIntent.ApplyPatternDraft -> handleApply()
            KLogViewerIntent.ResamplePatternLines -> handleResample()
            is KLogViewerIntent.AddPatternToken -> handleAddToken(intent)
            is KLogViewerIntent.RemovePatternSegment -> handleRemoveSegment(intent)
            is KLogViewerIntent.ReorderPatternSegment -> handleReorderSegment(intent)
            is KLogViewerIntent.UpdatePatternToken -> handleUpdateToken(intent)
            is KLogViewerIntent.UpdatePatternDelimiter -> handleUpdateDelimiter(intent)
            is KLogViewerIntent.ImportPatternString -> mutateDraft { parsePatternStringToDraft(intent.patternText) }
            is KLogViewerIntent.ExtractSpanAsPatternToken -> handleExtractSpanAsToken(intent)
            is KLogViewerIntent.SetDirectoryPersistenceEnabled -> mutateDraft {
                it.copy(isDirectoryPersistenceEnabled = intent.enabled)
            }
            KLogViewerIntent.ResetPatternToBestGuess -> handleResetToBestGuess()
            KLogViewerIntent.UndoPatternDraft -> handleUndo()
            KLogViewerIntent.RedoPatternDraft -> handleRedo()
            else -> handleUiState(intent)
        }
    }

    private fun handleUndo() {
        state.update { s ->
            val w = s.patternWizardState
            if (w.canUndo) s.copy(patternWizardState = w.copy(draftHistory = w.draftHistory.undo())) else s
        }
        schedulePreviewRecompute(debounceMs = 0L)
    }

    private fun handleRedo() {
        state.update { s ->
            val w = s.patternWizardState
            if (w.canRedo) s.copy(patternWizardState = w.copy(draftHistory = w.draftHistory.redo())) else s
        }
        schedulePreviewRecompute(debounceMs = 0L)
    }

    private fun handleApply() {
        val wizard = state.value.patternWizardState
        val targetWindowId = wizard.targetWindowId
            ?: state.value.tabs.flatMap { it.windows }.firstOrNull()?.id
        val draftToApply = wizard.currentDraft
        handleClose()
        if (targetWindowId != null) {
            onApplyDraft?.invoke(targetWindowId, draftToApply)
        }
    }

    private fun handleResample() {
        val wizard = state.value.patternWizardState
        val targetWindowId = wizard.targetWindowId
            ?: state.value.tabs.flatMap { it.windows }.firstOrNull()?.id
        if (targetWindowId != null && onResampleLines != null) {
            val newSampleLines = onResampleLines.invoke(targetWindowId)
            if (newSampleLines.isNotEmpty()) {
                state.update {
                    it.copy(
                        patternWizardState = it.patternWizardState.copy(
                            sampleLines = newSampleLines,
                            selectedLineIndex = 0
                        )
                    )
                }
                schedulePreviewRecompute(debounceMs = 0L)
            }
        }
    }

    private fun handleUiState(intent: KLogViewerIntent.PatternWizardIntent) {
        when (intent) {
            is KLogViewerIntent.SetHoveredPatternSegment -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(hoveredSegmentId = intent.segmentId))
            }
            is KLogViewerIntent.SetHoveredPatternColumn -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(hoveredColumnName = intent.columnName))
            }
            is KLogViewerIntent.SetFocusedPatternToken -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(focusedTokenId = intent.tokenId))
            }
            is KLogViewerIntent.OpenPatternTokenConfigPopover -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(activePopoverTokenId = intent.tokenId))
            }
            KLogViewerIntent.ClosePatternTokenConfigPopover -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(activePopoverTokenId = null))
            }
            is KLogViewerIntent.OpenPatternSelectionPopup -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(activeSelectionPopupRange = intent.range))
            }
            KLogViewerIntent.ClosePatternSelectionPopup -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(activeSelectionPopupRange = null))
            }
            is KLogViewerIntent.SelectPatternSampleLine -> state.update {
                val nextIdx = intent.lineIndex.coerceIn(0, (it.patternWizardState.sampleLines.size - 1).coerceAtLeast(0))
                it.copy(patternWizardState = it.patternWizardState.copy(selectedLineIndex = nextIdx))
            }
            KLogViewerIntent.TogglePatternDiagnosticsDrawer -> state.update {
                val current = it.patternWizardState.isDiagnosticsDrawerOpen
                it.copy(patternWizardState = it.patternWizardState.copy(isDiagnosticsDrawerOpen = !current))
            }
            is KLogViewerIntent.UpdatePatternDialogBounds -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(
                    windowWidth = intent.width,
                    windowHeight = intent.height,
                    splitterRatio = intent.splitterRatio
                ))
            }
            KLogViewerIntent.ExpandPatternBannerToFullWizard -> state.update {
                it.copy(patternWizardState = it.patternWizardState.copy(isBannerMode = false))
            }
            else -> Unit
        }
    }

    private fun handleOpen(intent: KLogViewerIntent.OpenPatternWizard) {
        val initialDraft = intent.initialDraft ?: defaultBestGuessDraft()
        state.update { currentState ->
            currentState.copy(
                pendingDialog = KLogViewerState.DialogType.PATTERN_WIZARD,
                patternWizardState = currentState.patternWizardState.copy(
                    isVisible = true,
                    isBannerMode = intent.isBannerMode,
                    targetWindowId = intent.targetWindowId,
                    sampleLines = intent.sampleLines,
                    selectedLineIndex = 0,
                    initialBestGuess = initialDraft,
                    draftHistory = currentState.patternWizardState.draftHistory.reset(initialDraft),
                    confidenceScore = if (intent.sampleLines.isNotEmpty()) CONFIDENCE_HIGH else CONFIDENCE_LOW,
                    matchedLineCount = intent.sampleLines.size,
                    totalSampleLineCount = intent.sampleLines.size
                )
            )
        }
        schedulePreviewRecompute(debounceMs = 0L)
    }

    private fun handleClose() {
        state.update {
            it.copy(
                pendingDialog = if (it.pendingDialog == KLogViewerState.DialogType.PATTERN_WIZARD) null else it.pendingDialog,
                patternWizardState = it.patternWizardState.copy(isVisible = false)
            )
        }
    }

    private fun handleAddToken(intent: KLogViewerIntent.AddPatternToken) {
        mutateDraft { current ->
            val newToken = PatternToken(role = intent.role)
            val newSegment = PatternSegment.Token(newToken)
            val currentSegments = current.segments.toMutableList()
            val targetIdx = intent.segmentIndex.coerceIn(0, currentSegments.size)
            currentSegments.add(targetIdx, newSegment)
            current.copy(segments = currentSegments)
        }
    }

    private fun handleRemoveSegment(intent: KLogViewerIntent.RemovePatternSegment) {
        mutateDraft { current -> current.copy(segments = current.segments.filterNot { it.id == intent.segmentId }) }
    }

    private fun handleReorderSegment(intent: KLogViewerIntent.ReorderPatternSegment) {
        mutateDraft { current ->
            val idx = current.segments.indexOfFirst { it.id == intent.segmentId }
            if (idx == -1) return@mutateDraft current
            val newIdx = if (intent.moveLeft) idx - 1 else idx + 1
            if (newIdx !in current.segments.indices) return@mutateDraft current

            val mutableSegments = current.segments.toMutableList()
            val temp = mutableSegments[idx]
            mutableSegments[idx] = mutableSegments[newIdx]
            mutableSegments[newIdx] = temp
            current.copy(segments = mutableSegments)
        }
    }

    private fun handleUpdateToken(intent: KLogViewerIntent.UpdatePatternToken) {
        mutateDraft { current ->
            current.copy(segments = current.segments.map {
                if (it is PatternSegment.Token && it.token.id == intent.token.id) {
                    PatternSegment.Token(intent.token)
                } else {
                    it
                }
            })
        }
    }

    private fun handleUpdateDelimiter(intent: KLogViewerIntent.UpdatePatternDelimiter) {
        mutateDraft { current ->
            current.copy(segments = current.segments.map {
                if (it is PatternSegment.Delimiter && it.delimiter.id == intent.delimiterId) {
                    PatternSegment.Delimiter(it.delimiter.copy(value = intent.newValue))
                } else {
                    it
                }
            })
        }
    }

    private fun handleExtractSpanAsToken(intent: KLogViewerIntent.ExtractSpanAsPatternToken) {
        mutateDraft { current ->
            val newToken = PatternToken(role = intent.role, customPropertyName = intent.customName)
            current.copy(segments = current.segments + PatternSegment.Token(newToken))
        }
    }

    private fun handleResetToBestGuess() {
        state.update { currentState ->
            val bestGuess = currentState.patternWizardState.initialBestGuess ?: return@update currentState
            currentState.copy(
                patternWizardState = currentState.patternWizardState.copy(
                    draftHistory = currentState.patternWizardState.draftHistory.push(bestGuess)
                )
            )
        }
        schedulePreviewRecompute(debounceMs = 0L)
    }

    private inline fun mutateDraft(transform: (PatternDraft) -> PatternDraft) {
        state.update { currentState ->
            val wizard = currentState.patternWizardState
            currentState.copy(
                patternWizardState = wizard.copy(
                    draftHistory = wizard.draftHistory.push(transform(wizard.currentDraft))
                )
            )
        }
        schedulePreviewRecompute(debounceMs = 150L)
    }

    fun schedulePreviewRecompute(debounceMs: Long = 150L) {
        val currentGeneration = ++previewGeneration
        val wizard = state.value.patternWizardState
        val currentDraft = wizard.currentDraft
        val sampleLines = wizard.sampleLines

        state.update {
            it.copy(patternWizardState = it.patternWizardState.copy(isComputingPreview = true))
        }

        previewJob?.cancel()
        previewJob = scope.launch(computationDispatcher) {
            if (debounceMs > 0) {
                delay(debounceMs)
            }
            val result = patternPreviewService.computePreview(currentDraft, sampleLines)
            if (currentGeneration == previewGeneration) {
                state.update { currentState ->
                    val currentWizard = currentState.patternWizardState
                    if (currentWizard.isVisible) {
                        currentState.copy(
                            patternWizardState = currentWizard.copy(
                                previewSpans = result.spansPerLine,
                                previewRows = result.previewRows,
                                previewColumns = result.columns,
                                parseErrors = result.parseErrors,
                                matchedLineCount = result.matchedLineCount,
                                totalSampleLineCount = result.totalSampleLineCount,
                                confidenceScore = result.confidenceScore,
                                isComputingPreview = false
                            )
                        )
                    } else currentState
                }
            }
        }
    }

    private fun defaultBestGuessDraft(): PatternDraft {
        val defaultSegments = listOf(
            PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")),
            PatternSegment.Delimiter(PatternDelimiter(value = " [")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
            PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
            PatternSegment.Delimiter(PatternDelimiter(value = " ")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER)),
            PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
        )
        return PatternDraft(
            name = "Logback / Log4J Default",
            segments = defaultSegments
        )
    }

    private val presetMap = mapOf(
        "Logback / Log4J Standard" to
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger - %msg",
        "Serilog Text Layout" to
            "{Timestamp:yyyy-MM-dd HH:mm:ss.SSS} [{Level}] [{ThreadId}] {SourceContext} - {Message}",
        "ISO8601 Simple" to
            "%d{yyyy-MM-ddTHH:mm:ss} %level %logger - %msg",
        "Custom Draft" to
            "%d{yyyy-MM-dd HH:mm:ss} %level [%t] %logger - %msg"
    )

    private fun parsePatternStringToDraft(patternText: String): PatternDraft {
        val trimmed = patternText.trim()
        val formatStr = presetMap[trimmed] ?: trimmed
        val segments = parseFormatStringToSegments(formatStr)
        val name = when {
            presetMap.containsKey(trimmed) -> trimmed
            trimmed.startsWith("{") -> "Serilog Text Layout"
            trimmed.startsWith("%") -> "Logback / Log4J Standard"
            else -> "Custom Pattern"
        }
        return PatternDraft(
            name = name,
            originalFormatString = formatStr,
            segments = segments
        )
    }

    private fun parseFormatStringToSegments(formatStr: String): List<PatternSegment> {
        if (formatStr.isBlank()) return defaultBestGuessDraft().segments
        val parsed = when {
            formatStr.contains("%") -> parseLogbackFormatString(formatStr)
            formatStr.contains("{") -> parseSerilogFormatString(formatStr)
            else -> defaultBestGuessDraft().segments
        }
        return parsed.ifEmpty { defaultBestGuessDraft().segments }
    }

    private fun parseLogbackFormatString(formatStr: String): List<PatternSegment> {
        val segments = mutableListOf<PatternSegment>()
        val regex = Regex(
            """(%d(?:\{[^}]*})?|%t(?:hread)?|%-?\d*level|%-?\d*p|""" +
                """%c(?:\{[^}]*})?|%logger(?:\{[^}]*})?|%m(?:sg)?|%n|%ex|%X\{[^}]+}|[^%]+)"""
        )
        val matches = regex.findAll(formatStr).map { it.value }.toList()

        matches.forEach { tokenStr ->
            val segment = parseLogbackToken(tokenStr)
            if (segment != null) {
                segments.add(segment)
            }
        }
        return segments
    }

    private fun parseLogbackToken(tokenStr: String): PatternSegment? {
        return when {
            tokenStr.startsWith("%d") -> {
                val dateFormat = Regex("""%d(?:\{([^}]*)})?""")
                    .find(tokenStr)?.groupValues?.get(1) ?: "yyyy-MM-dd HH:mm:ss.SSS"
                PatternSegment.Token(
                    PatternToken(
                        role = PatternTokenRole.TIMESTAMP,
                        formatPattern = dateFormat.ifBlank { "yyyy-MM-dd HH:mm:ss.SSS" }
                    )
                )
            }
            tokenStr.startsWith("%t") ->
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD))
            tokenStr.contains("level") || tokenStr.contains("p") ->
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL))
            tokenStr.startsWith("%c") || tokenStr.startsWith("%logger") ->
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER))
            tokenStr.startsWith("%m") ->
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            tokenStr.startsWith("%ex") ->
                PatternSegment.Token(PatternToken(role = PatternTokenRole.EXCEPTION))
            tokenStr.startsWith("%X{") -> {
                val propName = Regex("""%X\{([^}]+)}""").find(tokenStr)?.groupValues?.get(1) ?: "prop"
                PatternSegment.Token(
                    PatternToken(
                        role = PatternTokenRole.CUSTOM_PROPERTY,
                        customPropertyName = propName
                    )
                )
            }
            tokenStr == "%n" -> null
            else -> PatternSegment.Delimiter(PatternDelimiter(value = tokenStr))
        }
    }

    private fun parseSerilogFormatString(formatStr: String): List<PatternSegment> {
        val segments = mutableListOf<PatternSegment>()
        val regex = Regex("""(\{[^}]+}|[^{]+)""")
        val matches = regex.findAll(formatStr).map { it.value }.toList()

        matches.forEach { tokenStr ->
            segments.add(parseSerilogToken(tokenStr))
        }
        return segments
    }

    private fun parseSerilogToken(tokenStr: String): PatternSegment {
        if (!tokenStr.startsWith("{") || !tokenStr.endsWith("}")) {
            return PatternSegment.Delimiter(PatternDelimiter(value = tokenStr))
        }

        val inner = tokenStr.removeSurrounding("{", "}")
        val namePart = inner.split(":").first()
        return when (namePart.lowercase()) {
            "timestamp", "t" -> {
                val format = if (inner.contains(":")) inner.substringAfter(":") else "yyyy-MM-dd HH:mm:ss.SSS"
                PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = format))
            }
            "level", "l" -> PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL))
            "threadid", "thread" -> PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD))
            "sourcecontext", "logger" -> PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER))
            "message", "m", "msg" -> PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            "exception" -> PatternSegment.Token(PatternToken(role = PatternTokenRole.EXCEPTION))
            else -> PatternSegment.Token(
                PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = namePart)
            )
        }
    }

    private companion object {
        private const val CONFIDENCE_HIGH = 0.95f
        private const val CONFIDENCE_LOW = 0.6f
    }
}
