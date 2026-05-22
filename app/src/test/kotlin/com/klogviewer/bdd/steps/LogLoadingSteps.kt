package com.klogviewer.bdd.steps

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LogLoadingSteps {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    private val parser = SimpleLogParser()
    private val logSource = FileLogSource(parser, testDispatcher)
    private val tempDir = File("build/tmp/cucumber").apply { mkdirs() }
    private val prefsRepository = JsonPreferencesRepository(tempDir)
    private val parserRegistry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(parserRegistry)
    private lateinit var viewModel: KLogViewerViewModel

    @Given("a log file exists at {string} with content:")
    fun a_log_file_exists_at_with_content(path: String, content: String) {
        File(path).writeText(content)
    }

    @When("I load the log file {string}")
    fun i_load_the_log_file(path: String) = testScope.runTest {
        viewModel = KLogViewerViewModel(
            logSource = logSource, 
            prefsRepository = prefsRepository, 
            heuristicProbe = heuristicProbe, 
            scope = backgroundScope
        )
        
        // The default state already has a tab and a window, but we might need to ensure it's fresh
        advanceUntilIdle()
        
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path)))
        advanceUntilIdle()
    }

    @Then("I should see {int} log entries")
    fun i_should_see_log_entries(count: Int) {
        val state = viewModel.state.value
        val logs = state.activeTab?.activeWindow?.filteredLogs ?: emptyList()
        expectThat(logs.size).isEqualTo(count)
    }

    @Then("the first entry should have level {string} and content {string}")
    fun the_first_entry_should_have_level_and_content(level: String, content: String) {
        val state = viewModel.state.value
        val entry = state.activeTab?.activeWindow?.filteredLogs?.get(0)
        expectThat(entry?.level?.name).isEqualTo(level)
        expectThat(entry?.content?.value).isEqualTo(content)
    }

    @Then("the second entry should have level {string} and content {string}")
    fun the_second_entry_should_have_level_and_content(level: String, content: String) {
        val state = viewModel.state.value
        val entry = state.activeTab?.activeWindow?.filteredLogs?.get(1)
        expectThat(entry?.level?.name).isEqualTo(level)
        expectThat(entry?.content?.value).isEqualTo(content)
    }
}
