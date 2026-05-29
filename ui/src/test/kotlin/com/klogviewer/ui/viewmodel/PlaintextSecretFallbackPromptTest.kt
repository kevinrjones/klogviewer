package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.PreferencesRepository
import com.klogviewer.domain.repository.PreferencesSaveOptions
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.ui.mvi.KLogViewerIntent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@OptIn(ExperimentalCoroutinesApi::class)
class PlaintextSecretFallbackPromptTest {
    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<PreferencesRepository>()
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var viewModel: KLogViewerViewModel

    @BeforeEach
    fun setup() {
        testDispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
        every { prefsRepository.load() } returns UserPreferences()
        every { prefsRepository.save(any(), any()) } returns PreferencesSaveResult.Saved
    }

    @AfterEach
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.clear()
            testScope.advanceUntilIdle()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun `given secure store unavailable when saving then show plaintext confirmation prompt`() {
        every {
            prefsRepository.save(any(), PreferencesSaveOptions(allowPlaintextSecretFallback = false))
        } returns PreferencesSaveResult.RequiresPlaintextSecretConfirmation

        viewModel = KLogViewerViewModel(
            logSource = logSource,
            prefsRepository = prefsRepository,
            heuristicProbe = heuristicProbe,
            scope = testScope
        )

        viewModel.savePreferences()
        testScope.advanceUntilIdle()

        expectThat(viewModel.state.value.pendingPlaintextSecretSave).isNotNull()
    }

    @Test
    fun `given confirmation approved when retrying save then plaintext fallback is enabled`() {
        val optionsSlot = mutableListOf<PreferencesSaveOptions>()
        every { prefsRepository.save(any(), capture(optionsSlot)) } returnsMany listOf(
            PreferencesSaveResult.RequiresPlaintextSecretConfirmation,
            PreferencesSaveResult.Saved
        )

        viewModel = KLogViewerViewModel(
            logSource = logSource,
            prefsRepository = prefsRepository,
            heuristicProbe = heuristicProbe,
            scope = testScope
        )

        viewModel.savePreferences()
        viewModel.handleIntent(KLogViewerIntent.ConfirmPlaintextSecretSave)
        testScope.advanceUntilIdle()

        expectThat(optionsSlot.size).isEqualTo(2)
        expectThat(optionsSlot[0].allowPlaintextSecretFallback).isEqualTo(false)
        expectThat(optionsSlot[1].allowPlaintextSecretFallback).isEqualTo(true)
        expectThat(viewModel.state.value.pendingPlaintextSecretSave).isNull()
    }

    @Test
    fun `given confirmation declined when prompt shown then pending prompt is cleared without retry`() {
        every {
            prefsRepository.save(any(), PreferencesSaveOptions(allowPlaintextSecretFallback = false))
        } returns PreferencesSaveResult.RequiresPlaintextSecretConfirmation

        viewModel = KLogViewerViewModel(
            logSource = logSource,
            prefsRepository = prefsRepository,
            heuristicProbe = heuristicProbe,
            scope = testScope
        )

        viewModel.savePreferences()
        viewModel.handleIntent(KLogViewerIntent.DeclinePlaintextSecretSave)
        testScope.advanceUntilIdle()

        expectThat(viewModel.state.value.pendingPlaintextSecretSave).isNull()
        verify(exactly = 1) {
            prefsRepository.save(any(), PreferencesSaveOptions(allowPlaintextSecretFallback = false))
        }
        verify(exactly = 0) {
            prefsRepository.save(any(), PreferencesSaveOptions(allowPlaintextSecretFallback = true))
        }
    }
}