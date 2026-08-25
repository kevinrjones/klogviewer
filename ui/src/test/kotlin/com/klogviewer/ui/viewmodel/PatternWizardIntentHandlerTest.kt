package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class PatternWizardIntentHandlerTest {

    @Test
    fun `given open wizard intent when handled then state is visible and populated`() {
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(state)
        val samples = listOf("2026-08-25 10:00:00 [main] INFO App - Hello")

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = samples))

        expectThat(state.value.pendingDialog).isEqualTo(KLogViewerState.DialogType.PATTERN_WIZARD)
        expectThat(state.value.patternWizardState.isVisible).isTrue()
        expectThat(state.value.patternWizardState.sampleLines).isEqualTo(samples)
    }

    @Test
    fun `given open wizard when add token and undo then draft reverts`() {
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(state)

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Sample line")))
        val initialSegmentsCount = state.value.patternWizardState.currentDraft.segments.size

        handler.handle(KLogViewerIntent.AddPatternToken(0, PatternTokenRole.CUSTOM_PROPERTY))
        expectThat(state.value.patternWizardState.currentDraft.segments.size).isEqualTo(initialSegmentsCount + 1)
        expectThat(state.value.patternWizardState.canUndo).isTrue()

        handler.handle(KLogViewerIntent.UndoPatternDraft)
        expectThat(state.value.patternWizardState.currentDraft.segments.size).isEqualTo(initialSegmentsCount)
        expectThat(state.value.patternWizardState.canRedo).isTrue()

        handler.handle(KLogViewerIntent.RedoPatternDraft)
        expectThat(state.value.patternWizardState.currentDraft.segments.size).isEqualTo(initialSegmentsCount + 1)
    }

    @Test
    fun `given open wizard when close or skip handled then state closes non-destructively`() {
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(state)

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Sample line")))
        expectThat(state.value.patternWizardState.isVisible).isTrue()

        handler.handle(KLogViewerIntent.ClosePatternWizard)
        expectThat(state.value.patternWizardState.isVisible).isFalse()
        expectThat(state.value.pendingDialog).isNull()
    }

    @Test
    fun `given import pattern string when logback or serilog or preset imported then draft creates valid segments`() {
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(state)

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Sample line")))

        handler.handle(KLogViewerIntent.ImportPatternString("Serilog Text Layout"))
        val serilogDraft = state.value.patternWizardState.currentDraft
        expectThat(serilogDraft.name).isEqualTo("Serilog Text Layout")
        expectThat(serilogDraft.segments.isEmpty()).isFalse()

        handler.handle(KLogViewerIntent.ImportPatternString("Custom Draft"))
        val customDraft = state.value.patternWizardState.currentDraft
        expectThat(customDraft.name).isEqualTo("Custom Draft")
        expectThat(customDraft.segments.isEmpty()).isFalse()
    }
}
