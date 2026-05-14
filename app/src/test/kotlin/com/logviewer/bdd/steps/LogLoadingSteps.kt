package com.logviewer.bdd.steps

import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.core.repository.PreferencesRepository
import com.logviewer.core.source.FileLogSource
import com.logviewer.domain.model.LogLevel
import com.logviewer.ui.mvi.LogViewerIntent
import com.logviewer.ui.viewmodel.LogViewerViewModel
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import java.nio.file.Files

class LogLoadingSteps {
    private val tempPrefsDir = Files.createTempDirectory("logviewer-prefs").toFile().apply { deleteOnExit() }
    private val parser = SimpleLogParser()
    private val source = FileLogSource(parser)
    private val prefsRepository = PreferencesRepository(tempPrefsDir)
    private val viewModel = LogViewerViewModel(source, prefsRepository)

    @Given("a log file exists at {string} with content:")
    fun a_log_file_exists_at_with_content(path: String, content: String) {
        File(path).writeText(content)
    }

    @When("I load the log file {string}")
    fun i_load_the_log_file(path: String) {
        viewModel.handleIntent(LogViewerIntent.LoadFiles(listOf(path)))
        // Wait for loading to finish
        runBlocking {
            withTimeout(2000) {
                viewModel.state.first { 
                    val tab = it.activeTab
                    tab != null && !tab.isLoading && (tab.logs.isNotEmpty() || tab.error != null) 
                }
            }
        }
    }

    @Then("I should see {int} log entries")
    fun i_should_see_log_entries(count: Int) {
        val tab = viewModel.state.value.activeTab
        assertEquals(count, tab?.logs?.size ?: 0)
    }

    @Then("the first entry should have level {string} and content {string}")
    fun the_first_entry_should_have_level_and_content(level: String, content: String) {
        val entry = viewModel.state.value.activeTab!!.logs[0]
        assertEquals(level, entry.level.name)
        assertEquals(content, entry.content.value)
    }

    @Then("the second entry should have level {string} and content {string}")
    fun the_second_entry_should_have_level_and_content(level: String, content: String) {
        val entry = viewModel.state.value.activeTab!!.logs[1]
        assertEquals(level, entry.level.name)
        assertEquals(content, entry.content.value)
    }
}
