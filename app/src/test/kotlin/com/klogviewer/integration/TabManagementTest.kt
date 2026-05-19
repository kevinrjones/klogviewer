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

    @Test
    fun `should resize columns independently in different split windows`() = runBlocking {
        // 1. Create two splits
        viewModel.handleIntent(KLogViewerIntent.SplitHorizontal)
        val tab = viewModel.state.value.activeTab!!
        val firstWindowId = tab.windows[0].id
        val secondWindowId = tab.windows[1].id
        
        // Focus the second window
        viewModel.handleIntent(KLogViewerIntent.SwitchWindow(secondWindowId))
        assertEquals(secondWindowId, viewModel.state.value.activeTab?.activeWindowId)
        
        // 2. Resize column in the FIRST window (which is NOT focused)
        viewModel.handleIntent(KLogViewerIntent.UpdateColumnWidth(firstWindowId, "Level", 100))
        
        // 3. Verify FIRST window has the new width
        val w1 = viewModel.state.value.activeTab?.windows?.find { it.id == firstWindowId }!!
        assertEquals(100, w1.columnWidths["Level"])
        
        // 4. Verify SECOND window (focused) does NOT have that width
        val w2 = viewModel.state.value.activeTab?.windows?.find { it.id == secondWindowId }!!
        assertNotEquals(100, w2.columnWidths["Level"])
        
        // 5. Focus the first window and resize a column in the second window
        viewModel.handleIntent(KLogViewerIntent.SwitchWindow(firstWindowId))
        viewModel.handleIntent(KLogViewerIntent.UpdateColumnWidth(secondWindowId, "Message", 500))
        
        val w2Updated = viewModel.state.value.activeTab?.windows?.find { it.id == secondWindowId }!!
        assertEquals(500, w2Updated.columnWidths["Message"])
        
        val w1Check = viewModel.state.value.activeTab?.windows?.find { it.id == firstWindowId }!!
        assertNotEquals(500, w1Check.columnWidths["Message"])
    }

    @Test
    fun `should toggle all levels at once`() = runBlocking {
        val allLevels = com.klogviewer.domain.model.LogLevel.entries.toSet()
        
        // Initially all levels are filtered in (assuming default state)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All (to disable all)
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(emptySet<com.klogviewer.domain.model.LogLevel>(), viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All (to enable all)
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Deselect one level
        val infoLevel = com.klogviewer.domain.model.LogLevel.INFO
        viewModel.handleIntent(KLogViewerIntent.ToggleLevel(infoLevel))
        assertNotEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All should now enable all again because NOT all were selected
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
    }

    @Test
    fun `should support multi-selection via ToggleEntrySelection`() = runBlocking {
        val file = File.createTempFile("log1", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Line 1\n")
            appendText("2023-10-27 10:00:01 [INFO] Line 2\n")
            appendText("2023-10-27 10:00:02 [INFO] Line 3\n")
            appendText("2023-10-27 10:00:03 [INFO] Line 4\n")
            deleteOnExit()
        }
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file.absolutePath)))
        withTimeout(2000.milliseconds) {
            viewModel.state.first { it.activeTab?.activeWindow?.logs?.size == 4 }
        }
        
        // Select first line
        viewModel.handleIntent(KLogViewerIntent.ToggleEntrySelection(0))
        assertEquals(setOf(0), viewModel.state.value.activeTab?.activeWindow?.selectedIndices)
        
        // Meta+Select third line (Toggle)
        viewModel.handleIntent(KLogViewerIntent.ToggleEntrySelection(2, isMetaPressed = true))
        assertEquals(setOf(0, 2), viewModel.state.value.activeTab?.activeWindow?.selectedIndices)
        
        // Shift+Select fourth line (Range from last clicked index 2)
        viewModel.handleIntent(KLogViewerIntent.ToggleEntrySelection(3, isShiftPressed = true))
        // Should select 2 and 3. Set should be {0, 2, 3}
        assertEquals(setOf(0, 2, 3), viewModel.state.value.activeTab?.activeWindow?.selectedIndices)
        
        // Select without modifiers should clear others
        viewModel.handleIntent(KLogViewerIntent.ToggleEntrySelection(1))
        assertEquals(setOf(1), viewModel.state.value.activeTab?.activeWindow?.selectedIndices)
    }
}
