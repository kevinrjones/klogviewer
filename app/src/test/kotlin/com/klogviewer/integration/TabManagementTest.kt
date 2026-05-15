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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class TabManagementTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { PreferencesRepository(tempDir) }
    private val viewModel by lazy { KLogViewerViewModel(source, prefsRepository, heuristicProbe) }

    @Test
    fun `should maintain independent filter queries and logs per tab`() = runBlocking {
        // Create first tab with a log
        val file1 = File.createTempFile("log1", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Log entry one\n")
            deleteOnExit()
        }
        val tab1Id = viewModel.state.value.activeTabId!!
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        
        // Wait for load
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && (it.activeTab?.activeWindow?.logs?.isNotEmpty() ?: false) }
        }
        
        viewModel.handleIntent(KLogViewerIntent.AddFilterQuery("entry one"))
        assertEquals(1, viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.size)

        // Add second tab
        viewModel.handleIntent(KLogViewerIntent.AddTab)
        val tab2Id = viewModel.state.value.activeTabId!!
        assertNotEquals(tab1Id, tab2Id)
        
        // Load different log into second tab
        val file2 = File.createTempFile("log2", ".log").apply {
            writeText("2023-10-27 10:00:01 [ERROR] Something failed\n")
            deleteOnExit()
        }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file2.absolutePath)))
        
        // Wait for load
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.isLoading == false && (it.activeTab?.activeWindow?.logs?.isNotEmpty() ?: false) }
        }
        
        viewModel.handleIntent(KLogViewerIntent.AddFilterQuery("failed"))
        assertEquals(1, viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.size)
        
        // Switch back to tab 1
        viewModel.handleIntent(KLogViewerIntent.SwitchTab(tab1Id))
        assertEquals(listOf("entry one"), viewModel.state.value.activeTab?.activeWindow?.filterQueries)
        assertEquals(1, viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.size)
        assertEquals("Log entry one", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
        
        // Switch to tab 2
        viewModel.handleIntent(KLogViewerIntent.SwitchTab(tab2Id))
        assertEquals(listOf("failed"), viewModel.state.value.activeTab?.activeWindow?.filterQueries)
        assertEquals(1, viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.size)
        assertEquals("Something failed", viewModel.state.value.activeTab?.activeWindow?.filteredLogs?.get(0)?.content?.value)
    }

    @Test
    fun `should support multiple split windows in a single tab`() = runBlocking {
        val file1 = File.createTempFile("log1", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Log entry one\n")
            deleteOnExit()
        }
        
        // Load into first window
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.logs?.isNotEmpty() ?: false }
        }
        
        val firstWindowId = viewModel.state.value.activeTab?.activeWindowId!!
        
        // Split
        viewModel.handleIntent(KLogViewerIntent.SplitHorizontal)
        val secondWindowId = viewModel.state.value.activeTab?.activeWindowId!!
        assertNotEquals(firstWindowId, secondWindowId)
        assertEquals(2, viewModel.state.value.activeTab?.windows?.size)
        
        // Load into second window
        val file2 = File.createTempFile("log2", ".log").apply {
            writeText("2023-10-27 10:00:01 [ERROR] Something failed\n")
            deleteOnExit()
        }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file2.absolutePath)))
        withTimeout(2000.milliseconds) {
            viewModel.state.first { tabState ->
                tabState.activeTab?.windows?.find { it.id == secondWindowId }?.logs?.isNotEmpty() ?: false
            }
        }
        
        // Verify independent logs
        val tab = viewModel.state.value.activeTab!!
        val w1 = tab.windows.find { it.id == firstWindowId }!!
        val w2 = tab.windows.find { it.id == secondWindowId }!!
        
        assertEquals(1, w1.logs.size)
        assertEquals("Log entry one", w1.logs[0].content.value)
        
        assertEquals(1, w2.logs.size)
        assertEquals("Something failed", w2.logs[0].content.value)
        
        // Close second window
        viewModel.handleIntent(KLogViewerIntent.CloseWindow(secondWindowId))
        assertEquals(1, viewModel.state.value.activeTab?.windows?.size)
        assertEquals(firstWindowId, viewModel.state.value.activeTab?.activeWindowId)
    }
}
