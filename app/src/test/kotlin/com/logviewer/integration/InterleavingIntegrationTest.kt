package com.logviewer.integration

import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.core.repository.PreferencesRepository
import com.logviewer.core.source.FileLogSource
import com.logviewer.ui.mvi.LogViewerIntent
import com.logviewer.ui.viewmodel.LogViewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InterleavingIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { PreferencesRepository(tempDir) }
    private val viewModel by lazy { LogViewerViewModel(source, prefsRepository) }

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
        
        viewModel.handleIntent(LogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        withTimeout(2000) {
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
        viewModel.handleIntent(LogViewerIntent.AddToWorkspace(listOf(file2.absolutePath)))
        
        // Wait for merged load
        withTimeout(2000) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && it.activeTab?.activeWindow?.logs?.size == 4 }
        }
        
        val logs = viewModel.state.value.activeTab!!.activeWindow!!.logs
        assertEquals(4, logs.size)
        
        // Verify chronological order
        assertEquals("2023-10-27 10:00:00", logs[0].timestamp.value)
        assertEquals(file1.name, logs[0].sourceId)
        
        assertEquals("2023-10-27 10:00:01", logs[1].timestamp.value)
        assertEquals(file2.name, logs[1].sourceId)
        
        assertEquals("2023-10-27 10:00:02", logs[2].timestamp.value)
        assertEquals(file1.name, logs[2].sourceId)
        
        assertEquals("2023-10-27 10:00:03", logs[3].timestamp.value)
        assertEquals(file2.name, logs[3].sourceId)
    }
}
