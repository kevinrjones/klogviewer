package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isNotNull
import java.io.File
import kotlin.time.Duration.Companion.seconds

class LogLoadingIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    private var viewModel: KLogViewerViewModel? = null

    @AfterEach
    fun tearDown() {
        viewModel?.clear()
    }

    @Test
    fun `should load a single log file from disk into active log window`(): Unit = runBlocking {
        val logFile = File(tempDir, "single.log")
        logFile.writeText(
            """
            2024-05-12 10:00:00 [INFO] First message
            2024-05-12 10:00:01 [ERROR] Second message
            """.trimIndent()
        )

        val vm = createViewModel()
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(logFile.absolutePath)))

        withTimeout(5.seconds) {
            vm.state.first {
                val window = it.activeTab?.activeWindow
                window != null && !window.isLoading && window.logs.size == 2
            }
        }

        val window = vm.state.value.activeTab?.activeWindow
        expectThat(window).isNotNull()
        expectThat(window!!.logs).hasSize(2)
        expectThat(window.logs.map { it.content.value }).contains("First message", "Second message")
    }

    @Test
    fun `should load all log files in a directory from disk into active log window`(): Unit = runBlocking {
        val logDirectory = File(tempDir, "logs")
        logDirectory.mkdirs()

        val first = File(logDirectory, "first.log").apply {
            writeText("2024-05-12 10:00:00 [INFO] First directory message\n")
        }
        val second = File(logDirectory, "second.log").apply {
            writeText("2024-05-12 10:00:01 [WARN] Second directory message\n")
        }
        val third = File(logDirectory, "third.log").apply {
            writeText("2024-05-12 10:00:02 [ERROR] Third directory message\n")
        }

        val vm = createViewModel()
        vm.handleIntent(KLogViewerIntent.LoadFiles(listOf(logDirectory.absolutePath)))

        withTimeout(8.seconds) {
            vm.state.first {
                val window = it.activeTab?.activeWindow
                window != null && !window.isLoading && window.logs.size == 3
            }
        }

        val window = vm.state.value.activeTab?.activeWindow
        expectThat(window).isNotNull()
        expectThat(window!!.logs).hasSize(3)
        expectThat(window.logs.map { it.sourceId }.distinct()).hasSize(3)
        expectThat(window.logs.map { it.sourceId }).contains(first.absolutePath, second.absolutePath, third.absolutePath)
        expectThat(window.logs.map { it.content.value }).contains(
            "First directory message",
            "Second directory message",
            "Third directory message"
        )
    }

    private fun createViewModel(): KLogViewerViewModel {
        val prefsRepo = JsonPreferencesRepository(File(tempDir, "prefs"))
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)

        return KLogViewerViewModel(source, prefsRepo, heuristicProbe).also {
            viewModel = it
        }
    }
}