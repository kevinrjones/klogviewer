package com.klogviewer.ui.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.robot.mainRobot
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class KLogViewerSmokeTest {

    private val logSource = mockk<LogSource>(relaxed = true)
    private val prefsRepository = mockk<JsonPreferencesRepository>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>(relaxed = true)

    @Test
    fun app_launches_and_shows_main_components() = runComposeUiTest {
        every { prefsRepository.load() } returns UserPreferences()

        val viewModel = KLogViewerViewModel(logSource, prefsRepository, heuristicProbe)

        setContent {
            KLogViewerScreen(viewModel)
        }

        mainRobot {
            assertIsDisplayed("tab_row")
            assertIsDisplayed("add_tab_button")
            assertIsDisplayed("sidebar")
            assertIsDisplayed("filter_bar")
        }
    }
}
