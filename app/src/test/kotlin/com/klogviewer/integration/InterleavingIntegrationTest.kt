package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
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

class InterleavingIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { PreferencesRepository(tempDir) }
    private val viewModel by lazy { KLogViewerViewModel(source, prefsRepository, heuristicProbe) }

    @AfterEach
    fun tearDown() {
        viewModel.clear()
    }

    @Test
    fun `should interleave logs when adding to workspace`() = runBlocking {
        // Create first log file
        val file1 = File.createTempFile("log1", ".log").apply {
            writeText("""
                2023-10-27 10:00:00 [INFO] From File 1
                2023-10-27 10:00:02 [INFO] From File 1 again
            """.trimIndent())
            deleteOnExit()
        }
        
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && (it.activeTab?.activeWindow?.logs?.isNotEmpty() ?: false) }
        }
        
        assertEquals(2, viewModel.state.value.activeTab?.activeWindow?.logs?.size)

        // Create second log file
        val file2 = File.createTempFile("log2", ".log").apply {
            writeText("""
                2023-10-27 10:00:01 [INFO] From File 2
                2023-10-27 10:00:03 [INFO] From File 2 again
            """.trimIndent())
            deleteOnExit()
        }
        
        // Add to workspace
        viewModel.handleIntent(KLogViewerIntent.AddToWorkspace(listOf(file2.absolutePath)))
        
        // Wait for merged load
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && it.activeTab?.activeWindow?.logs?.size == 4 }
        }
        
        val logs = viewModel.state.value.activeTab!!.activeWindow!!.logs
        assertEquals(4, logs.size)
        
        // Verify chronological order
        assertEquals("2023-10-27 10:00:00", logs[0].timestamp.value)
        assertEquals(file1.absolutePath, logs[0].sourceId)
        
        assertEquals("2023-10-27 10:00:01", logs[1].timestamp.value)
        assertEquals(file2.absolutePath, logs[1].sourceId)
        
        assertEquals("2023-10-27 10:00:02", logs[2].timestamp.value)
        assertEquals(file1.absolutePath, logs[2].sourceId)
        
        assertEquals("2023-10-27 10:00:03", logs[3].timestamp.value)
        assertEquals(file2.absolutePath, logs[3].sourceId)
    }
}
