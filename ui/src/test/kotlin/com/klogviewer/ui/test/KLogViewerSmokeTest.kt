package com.klogviewer.ui.test

import androidx.compose.ui.test.junit4.createComposeRule
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.mainRobot
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class KLogViewerSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<PreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)

    @Test
    fun app_launches_and_shows_main_components() {
        every { prefsRepository.load() } returns UserPreferences()

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        composeTestRule.setContent {
            KLogViewerScreen(viewModel)
        }

        composeTestRule.mainRobot {
            assertIsDisplayed("tab_row")
            assertIsDisplayed("add_tab_button")
            assertIsDisplayed("sidebar")
            assertIsDisplayed("filter_bar")
        }
    }
}
