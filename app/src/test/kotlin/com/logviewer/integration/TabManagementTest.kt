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
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TabManagementTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { PreferencesRepository(tempDir) }
    private val viewModel by lazy { LogViewerViewModel(source, prefsRepository) }

    @Test
    fun `should maintain independent filter queries and logs per tab`() = runBlocking {
        // Create first tab with a log
        val file1 = File.createTempFile("log1", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Log entry one\n")
            deleteOnExit()
        }
        val tab1Id = viewModel.state.value.activeTabId!!
        viewModel.handleIntent(LogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        
        // Wait for load
        withTimeout(2000) {
            viewModel.state.first { !it.activeTab!!.isLoading && it.activeTab!!.logs.isNotEmpty() }
        }
        
        viewModel.handleIntent(LogViewerIntent.AddFilterQuery("entry one"))
        assertEquals(1, viewModel.state.value.activeTab?.filteredLogs?.size)

        // Add second tab
        viewModel.handleIntent(LogViewerIntent.AddTab)
        val tab2Id = viewModel.state.value.activeTabId!!
        assertNotEquals(tab1Id, tab2Id)
        
        // Load different log into second tab
        val file2 = File.createTempFile("log2", ".log").apply {
            writeText("2023-10-27 10:00:01 [ERROR] Something failed\n")
            deleteOnExit()
        }
        viewModel.handleIntent(LogViewerIntent.LoadFiles(listOf(file2.absolutePath)))
        
        // Wait for load
        withTimeout(2000) {
            viewModel.state.first { !it.activeTab!!.isLoading && it.activeTab!!.logs.isNotEmpty() }
        }
        
        viewModel.handleIntent(LogViewerIntent.AddFilterQuery("failed"))
        assertEquals(1, viewModel.state.value.activeTab?.filteredLogs?.size)
        
        // Switch back to tab 1
        viewModel.handleIntent(LogViewerIntent.SwitchTab(tab1Id))
        assertEquals(listOf("entry one"), viewModel.state.value.activeTab?.filterQueries)
        assertEquals(1, viewModel.state.value.activeTab?.filteredLogs?.size)
        assertEquals("Log entry one", viewModel.state.value.activeTab?.filteredLogs?.get(0)?.content?.value)
        
        // Switch to tab 2
        viewModel.handleIntent(LogViewerIntent.SwitchTab(tab2Id))
        assertEquals(listOf("failed"), viewModel.state.value.activeTab?.filterQueries)
        assertEquals(1, viewModel.state.value.activeTab?.filteredLogs?.size)
        assertEquals("Something failed", viewModel.state.value.activeTab?.filteredLogs?.get(0)?.content?.value)
    }
}
