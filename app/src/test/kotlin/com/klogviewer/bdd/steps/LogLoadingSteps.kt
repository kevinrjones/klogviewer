package com.klogviewer.bdd.steps

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.time.Duration.Companion.milliseconds

class LogLoadingSteps {
    private val tempPrefsDir = Files.createTempDirectory("klogviewer-prefs").toFile().apply { deleteOnExit() }
    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository = PreferencesRepository(tempPrefsDir)
    private val viewModel = KLogViewerViewModel(source, prefsRepository, heuristicProbe)

    @Given("a log file exists at {string} with content:")
    fun a_log_file_exists_at_with_content(path: String, content: String) {
        File(path).writeText(content)
    }

    @When("I load the log file {string}")
    fun i_load_the_log_file(path: String) {
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path)))
        // Wait for loading to finish
        runBlocking {
            withTimeout(2000.milliseconds) {
                viewModel.state.first { 
                    val window = it.activeTab?.activeWindow
                    window != null && !window.isLoading && (window.logs.isNotEmpty() || window.error != null) 
                }
            }
        }
    }

    @Then("I should see {int} log entries")
    fun i_should_see_log_entries(count: Int) {
        val window = viewModel.state.value.activeTab?.activeWindow
        assertEquals(count, window?.logs?.size ?: 0)
    }

    @Then("the first entry should have level {string} and content {string}")
    fun the_first_entry_should_have_level_and_content(level: String, content: String) {
        val entry = viewModel.state.value.activeTab!!.activeWindow!!.logs[0]
        assertEquals(level, entry.level.name)
        assertEquals(content, entry.content.value)
    }

    @Then("the second entry should have level {string} and content {string}")
    fun the_second_entry_should_have_level_and_content(level: String, content: String) {
        val entry = viewModel.state.value.activeTab!!.activeWindow!!.logs[1]
        assertEquals(level, entry.level.name)
        assertEquals(content, entry.content.value)
    }
}
