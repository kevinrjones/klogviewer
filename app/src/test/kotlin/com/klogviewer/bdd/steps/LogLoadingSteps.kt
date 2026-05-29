package com.klogviewer.bdd.steps

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LogLoadingSteps {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var scenarioScope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var prefsRepository: JsonPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scenarioScope = CoroutineScope(SupervisorJob() + testDispatcher)
        tempDir = File("build/tmp/cucumber/${System.nanoTime()}").apply { mkdirs() }
        prefsRepository = JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore())
    }

    @After
    fun tearDown() {
        scenarioScope.cancel()
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }
    private val parser = SimpleLogParser()
    private val logSource = FileLogSource(parser, testDispatcher)
    private val parserRegistry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(parserRegistry)
    private lateinit var viewModel: KLogViewerViewModel
    private var loadedFilePath: String? = null

    @Given("a log file exists at {string} with content:")
    fun a_log_file_exists_at_with_content(path: String, content: String) {
        File(path).writeText(content)
    }

    @When("I load the log file {string}")
    fun i_load_the_log_file(path: String) {
        loadedFilePath = path
        viewModel = KLogViewerViewModel(
            logSource = logSource, 
            prefsRepository = prefsRepository, 
            heuristicProbe = heuristicProbe, 
            scope = scenarioScope
        )

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path)))
    }

    @Then("I should see {int} log entries")
    fun i_should_see_log_entries(count: Int) {
        waitUntil {
            val logs = loadedWindow()?.logs ?: emptyList()
            logs.size == count
        }

        val logs = loadedWindow()?.logs ?: emptyList()
        expectThat(logs.size).isEqualTo(count)
    }

    @Then("the first entry should have level {string} and content {string}")
    fun the_first_entry_should_have_level_and_content(level: String, content: String) {
        waitUntil {
            val logs = loadedWindow()?.logs ?: emptyList()
            logs.size >= 1
        }

        val entry = loadedWindow()?.logs?.get(0)
        expectThat(entry?.level?.name).isEqualTo(level)
        expectThat(entry?.content?.value).isEqualTo(content)
    }

    @Then("the second entry should have level {string} and content {string}")
    fun the_second_entry_should_have_level_and_content(level: String, content: String) {
        waitUntil {
            val logs = loadedWindow()?.logs ?: emptyList()
            logs.size >= 2
        }

        val entry = loadedWindow()?.logs?.get(1)
        expectThat(entry?.level?.name).isEqualTo(level)
        expectThat(entry?.content?.value).isEqualTo(content)
    }

    private fun loadedWindow(): LogWindow? {
        val windows = viewModel.state.value.tabs.flatMap { it.windows }
        return loadedFilePath
            ?.let { path -> windows.find { window -> window.filePath == path } }
            ?: windows.firstOrNull { window -> window.logs.isNotEmpty() || window.filteredLogs.isNotEmpty() }
    }

    private fun waitUntil(
        timeoutMillis: Long = 5_000,
        pollIntervalMillis: Long = 10,
        predicate: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return
            Thread.sleep(pollIntervalMillis)
        }
        throw AssertionError("Condition was not met in time")
    }
}
