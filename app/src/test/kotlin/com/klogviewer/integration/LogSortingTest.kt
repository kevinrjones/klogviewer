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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class LogSortingTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { JsonPreferencesRepository(tempDir) }
    private val viewModel by lazy { KLogViewerViewModel(source, prefsRepository, heuristicProbe) }

    @AfterEach
    fun tearDown() {
        viewModel.clear()
    }

    @Test
    fun `should reverse logs when isReversed is toggled`() = runBlocking {
        val file = File.createTempFile("sorting", ".log").apply {
            writeText(
                """
                2023-10-27 10:00:00 [INFO] Entry 1
                2023-10-27 10:00:01 [INFO] Entry 2
                2023-10-27 10:00:02 [INFO] Entry 3
                """.trimIndent()
            )
            deleteOnExit()
        }
        
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file.absolutePath)))
        
        // Wait for load and filtering
        withTimeout(2000.milliseconds) {
            viewModel.state.first { 
                it.activeTab?.activeWindow?.isLoading == false && 
                it.activeTab?.activeWindow?.logs?.size == 3 &&
                it.activeTab?.activeWindow?.filteredLogs?.size == 3
            }
        }
        
        // Default order (Oldest First)
        assertEquals("Entry 1", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
        assertEquals("Entry 3", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(2)?.content?.value)
        
        // Toggle sort order
        viewModel.handleIntent(KLogViewerIntent.ToggleSortOrder)
        
        // Wait for filtering/sorting (it's on Dispatchers.Default)
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isReversed == true && it.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value == "Entry 3" }
        }
        
        assertEquals("Entry 3", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
        assertEquals("Entry 1", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(2)?.content?.value)
        
        // Toggle back
        viewModel.handleIntent(KLogViewerIntent.ToggleSortOrder)
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isReversed == false && it.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value == "Entry 1" }
        }
        assertEquals("Entry 1", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
    }

    @Test
    fun `should respect reverse order when logs are appended`() = runBlocking {
        val file = File.createTempFile("sorting-append", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Entry 1\n")
            deleteOnExit()
        }

        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file.absolutePath)))

        // Wait for load
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && it.activeTab?.activeWindow?.logs?.size == 1 }
        }

        // Toggle reverse
        viewModel.handleIntent(KLogViewerIntent.ToggleSortOrder)
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isReversed == true }
        }

        // Append log
        file.appendText("2023-10-27 10:00:01 [INFO] Entry 2\n")

        // Wait for append (FileLogSource polls every 1s)
        withTimeout(5000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.filteredLogs?.size == 2 }
        }

        // Verify that Entry 2 is at the top (index 0)
        assertEquals("Entry 2", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
        assertEquals("Entry 1", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(1)?.content?.value)
    }
}
