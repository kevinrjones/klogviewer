package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEmpty
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PatternWizardIntentHandlerTest {

    @Test
    fun `given open wizard intent when handled then state is visible and populated`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(
            state = state,
            scope = testScope,
            computationDispatcher = testDispatcher
        )
        val samples = listOf("2026-08-25 10:00:00.123 [main] INFO App - Hello")

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = samples))
        testScope.advanceTimeBy(1)

        expectThat(state.value.pendingDialog).isEqualTo(KLogViewerState.DialogType.PATTERN_WIZARD)
        expectThat(state.value.patternWizardState.isVisible).isTrue()
        expectThat(state.value.patternWizardState.sampleLines).isEqualTo(samples)
        expectThat(state.value.patternWizardState.previewRows).isNotEmpty()
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

        handler.handle(KLogViewerIntent.ImportPatternString("{yyyy-MM-dd HH:mm:ss.SSS +00:00} [{Level}] {Message}"))
        val pastedDraft = state.value.patternWizardState.currentDraft
        expectThat(pastedDraft.segments.size).isEqualTo(5)
        val firstToken = (pastedDraft.segments[0] as com.klogviewer.domain.model.PatternSegment.Token).token
        expectThat(firstToken.role).isEqualTo(PatternTokenRole.TIMESTAMP)
        expectThat(firstToken.formatPattern).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS +00:00")
        expectThat(firstToken.effectiveName).isEqualTo("timestamp")
    }

    @Test
    fun `given draft mutation when debounced preview finishes then preview rows and spans are updated`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val state = MutableStateFlow(KLogViewerState())
        val handler = PatternWizardIntentHandler(
            state = state,
            scope = testScope,
            computationDispatcher = testDispatcher
        )

        val samples = listOf("2026-08-25 10:00:00.123 [main] INFO App - Hello")
        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = samples))
        testScope.advanceTimeBy(1)

        handler.handle(KLogViewerIntent.AddPatternToken(0, PatternTokenRole.CUSTOM_PROPERTY))
        testScope.advanceTimeBy(100) // within 150ms debounce
        // Not completed yet
        testScope.advanceTimeBy(60) // after 150ms debounce
        expectThat(state.value.patternWizardState.isComputingPreview).isFalse()
    }

    @Test
    fun `given resample intent when handled then sample lines and preview are refreshed`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val state = MutableStateFlow(KLogViewerState())
        val resampled = listOf("2026-08-25 10:00:00.123 [main] INFO Resampled - Line 1")
        val handler = PatternWizardIntentHandler(
            state = state,
            scope = testScope,
            computationDispatcher = testDispatcher,
            onResampleLines = { resampled }
        )

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Old line"), targetWindowId = "w1"))
        testScope.advanceTimeBy(1)

        handler.handle(KLogViewerIntent.ResamplePatternLines)
        testScope.advanceTimeBy(1)

        expectThat(state.value.patternWizardState.sampleLines).isEqualTo(resampled)
    }

    @Test
    fun `given apply intent when handled then wizard closes and callback is invoked`() {
        val state = MutableStateFlow(KLogViewerState())
        var appliedWindowId: String? = null
        var appliedDraft: PatternDraft? = null

        val handler = PatternWizardIntentHandler(
            state = state,
            onApplyDraft = { wId, draft ->
                appliedWindowId = wId
                appliedDraft = draft
            }
        )

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Line"), targetWindowId = "window-123"))
        handler.handle(KLogViewerIntent.ApplyPatternDraft)

        expectThat(state.value.patternWizardState.isVisible).isFalse()
        expectThat(appliedWindowId).isEqualTo("window-123")
        expectThat(appliedDraft).isEqualTo(state.value.patternWizardState.currentDraft)
    }

    @Test
    fun `given delete directory mapping intent when handled then mapping is removed and preferences saved`() {
        val mapping = com.klogviewer.domain.model.DirectoryPatternMapping(
            directoryKey = "local:/var/log",
            patternDraft = PatternDraft(name = "Saved Pattern")
        )
        val state = MutableStateFlow(
            KLogViewerState(
                directoryPatternMappings = mapOf("local:/var/log" to mapping)
            )
        )
        var savedPrefs = false
        val handler = PatternWizardIntentHandler(
            state = state,
            onSavePreferences = { savedPrefs = true }
        )

        handler.handle(KLogViewerIntent.DeleteDirectoryPatternMapping("local:/var/log"))

        expectThat(state.value.directoryPatternMappings).isEqualTo(emptyMap())
        expectThat(savedPrefs).isTrue()
    }

    @Test
    fun `given save current draft as directory mapping intent when handled then mapping is stored`() {
        val state = MutableStateFlow(KLogViewerState())
        var savedPrefs = false
        val handler = PatternWizardIntentHandler(
            state = state,
            onSavePreferences = { savedPrefs = true }
        )

        handler.handle(KLogViewerIntent.OpenPatternWizard(sampleLines = listOf("Line")))
        handler.handle(KLogViewerIntent.SaveCurrentDraftAsDirectoryMapping("local:/var/log/app"))

        expectThat(state.value.directoryPatternMappings.containsKey("local:/var/log/app")).isTrue()
        expectThat(savedPrefs).isTrue()
    }

    @Test
    fun `given open directory mapping in wizard when handled then wizard opens with saved draft`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val draft = PatternDraft(name = "Saved Custom Pattern")
        val mapping = com.klogviewer.domain.model.DirectoryPatternMapping(
            directoryKey = "local:/var/log/app",
            patternDraft = draft
        )
        val state = MutableStateFlow(
            KLogViewerState(
                directoryPatternMappings = mapOf("local:/var/log/app" to mapping)
            )
        )
        val handler = PatternWizardIntentHandler(
            state = state,
            scope = testScope,
            computationDispatcher = testDispatcher
        )

        handler.handle(KLogViewerIntent.OpenDirectoryMappingInWizard("local:/var/log/app"))
        testScope.advanceTimeBy(1)

        expectThat(state.value.patternWizardState.isVisible).isTrue()
        expectThat(state.value.patternWizardState.currentDraft.name).isEqualTo("Saved Custom Pattern")
    }

    @Test
    fun `given open wizard intent without sample lines when target window set then samples fetched`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val state = MutableStateFlow(KLogViewerState())
        val rawLines = listOf("2026-08-25 10:00:00.123 [main] INFO RawService - Full line")
        val handler = PatternWizardIntentHandler(
            state = state,
            scope = testScope,
            computationDispatcher = testDispatcher,
            onResampleLines = { rawLines }
        )

        handler.handle(KLogViewerIntent.OpenPatternWizard(targetWindowId = "window-1"))
        testScope.advanceTimeBy(1)

        expectThat(state.value.patternWizardState.isVisible).isTrue()
        expectThat(state.value.patternWizardState.sampleLines).isEqualTo(rawLines)
    }
}
