package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class PatternWizardIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>
) {
    fun handle(intent: KLogViewerIntent.PatternWizardIntent) {
        when (intent) {
            is KLogViewerIntent.OpenPatternWizard -> handleOpen(intent)
            KLogViewerIntent.ClosePatternWizard,
            KLogViewerIntent.SkipPatternWizard,
            KLogViewerIntent.ApplyPatternDraft -> handleClose()
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
    }

    private fun handleRedo() {
        state.update { s ->
            val w = s.patternWizardState
            if (w.canRedo) s.copy(patternWizardState = w.copy(draftHistory = w.draftHistory.redo())) else s
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
        return PatternDraft(name = "Default Guess", segments = defaultSegments, originalFormatString = DEFAULT_FORMAT_PATTERN)
    }

    private fun parsePatternStringToDraft(text: String): PatternDraft {
        val segments = mutableListOf<PatternSegment>()
        if (text.contains("%d") || text.contains("%level") || text.contains("%msg")) {
            segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP)))
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = " ")))
            segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)))
            segments.add(PatternSegment.Delimiter(PatternDelimiter(value = " ")))
            segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE)))
        } else {
            segments.add(PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE)))
        }
        return PatternDraft(name = "Pasted Pattern", originalFormatString = text, segments = segments)
    }

    companion object {
        private const val CONFIDENCE_HIGH = 0.95f
        private const val CONFIDENCE_LOW = 0.5f
        private const val DEFAULT_FORMAT_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger - %msg"
    }
}
