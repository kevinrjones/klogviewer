package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.LevelFilterKey
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.AfterEach
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
    private val prefsRepository by lazy { JsonPreferencesRepository(tempDir, InMemorySecureCredentialStore()) }
    private val viewModel by lazy { KLogViewerViewModel(source, prefsRepository, heuristicProbe) }

    @AfterEach
    fun tearDown() {
        viewModel.clear()
    }

    @Test
    fun `should maintain independent filter queries and logs per tab`() = runBlocking {
        // Create first tab with a log
        val file1 = createTempLogFile(
            "log1",
            "2023-10-27 10:00:00 [INFO] Log entry one\n"
        )
        val tab1Id = viewModel.state.value.activeTabId!!
        val tab1WindowId = viewModel.state.value.activeTab?.activeWindowId!!
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath)))

        waitUntilWindowLoaded(tab1Id, tab1WindowId, expectedLogCount = 1)
        
        viewModel.handleIntent(KLogViewerIntent.AddFilterQuery("entry one"))
        waitUntilFilterApplied(tab1Id, tab1WindowId, expectedQueries = listOf("entry one"), expectedFilteredCount = 1)

        assertEquals(1, windowState(tab1Id, tab1WindowId)?.filteredLogs?.size)

        // Add second tab
        viewModel.handleIntent(KLogViewerIntent.AddTab)
        val tab2Id = viewModel.state.value.activeTabId!!
        val tab2WindowId = viewModel.state.value.activeTab?.activeWindowId!!
        assertNotEquals(tab1Id, tab2Id)
        
        // Load different log into second tab
        val file2 = createTempLogFile(
            "log2",
            "2023-10-27 10:00:01 [ERROR] Something failed\n"
        )
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file2.absolutePath)))

        waitUntilWindowLoaded(tab2Id, tab2WindowId, expectedLogCount = 1)
        
        viewModel.handleIntent(KLogViewerIntent.AddFilterQuery("failed"))
        waitUntilFilterApplied(tab2Id, tab2WindowId, expectedQueries = listOf("failed"), expectedFilteredCount = 1)

        assertEquals(1, windowState(tab2Id, tab2WindowId)?.filteredLogs?.size)
        
        // Switch back to tab 1
        viewModel.handleIntent(KLogViewerIntent.SwitchTab(tab1Id))
        waitUntilActiveTab(tab1Id)
        assertEquals(listOf("entry one"), windowState(tab1Id, tab1WindowId)?.filterQueries)
        assertEquals(1, windowState(tab1Id, tab1WindowId)?.filteredLogs?.size)
        assertEquals("Log entry one", windowState(tab1Id, tab1WindowId)?.filteredLogs?.get(0)?.content?.value)
        
        // Switch to tab 2
        viewModel.handleIntent(KLogViewerIntent.SwitchTab(tab2Id))
        waitUntilActiveTab(tab2Id)
        assertEquals(listOf("failed"), windowState(tab2Id, tab2WindowId)?.filterQueries)
        assertEquals(1, windowState(tab2Id, tab2WindowId)?.filteredLogs?.size)
        assertEquals("Something failed", windowState(tab2Id, tab2WindowId)?.filteredLogs?.get(0)?.content?.value)
    }

    @Test
    fun `should support multiple split windows in a single tab`() = runBlocking {
        val file1 = createTempLogFile(
            "log1",
            "2023-10-27 10:00:00 [INFO] Log entry one\n"
        )
        val tabId = viewModel.state.value.activeTabId!!
        val firstWindowId = viewModel.state.value.activeTab?.activeWindowId!!
        
        // Load into first window
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file1.absolutePath)))
        waitUntilWindowLoaded(tabId, firstWindowId, expectedLogCount = 1)
        
        // Split
        viewModel.handleIntent(KLogViewerIntent.SplitHorizontal)
        val secondWindowId = viewModel.state.value.activeTab?.activeWindowId!!
        assertNotEquals(firstWindowId, secondWindowId)
        assertEquals(2, viewModel.state.value.activeTab?.windows?.size)
        
        // Load into second window
        val file2 = createTempLogFile(
            "log2",
            "2023-10-27 10:00:01 [ERROR] Something failed\n"
        )
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file2.absolutePath)))
        waitUntilWindowLoaded(tabId, secondWindowId, expectedLogCount = 1)
        
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
        val allLevels = LevelFilterKey.defaults
        
        // Initially all levels are filtered in (assuming default state)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All (to disable all)
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(emptySet<LevelFilterKey>(), viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All (to enable all)
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Deselect one level
        val infoLevel = com.klogviewer.domain.model.LogLevel.INFO
        viewModel.handleIntent(KLogViewerIntent.ToggleLevel(LevelFilterKey.fromLogLevel(infoLevel)))
        assertNotEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
        
        // Toggle All should now enable all again because NOT all were selected
        viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels)
        assertEquals(allLevels, viewModel.state.value.activeTab?.activeWindow?.levelFilters)
    }

    @Test
    fun `should support multi-selection via ToggleEntrySelection`() = runBlocking {
        val file = createTempLogFile(
            "log1",
            """
            2023-10-27 10:00:00 [INFO] Line 1
            2023-10-27 10:00:01 [INFO] Line 2
            2023-10-27 10:00:02 [INFO] Line 3
            2023-10-27 10:00:03 [INFO] Line 4
            """.trimIndent() + "\n"
        )
        val tabId = viewModel.state.value.activeTabId!!
        val windowId = viewModel.state.value.activeTab?.activeWindowId!!
        viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(file.absolutePath)))
        waitUntilWindowLoaded(tabId, windowId, expectedLogCount = 4)
        
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

    private fun createTempLogFile(prefix: String, content: String): File {
        val file = File(tempDir, "$prefix-${System.nanoTime()}.log")
        file.writeText(content)
        return file
    }

    private fun windowState(tabId: String, windowId: String): LogWindow? {
        val tab = viewModel.state.value.tabs.find { it.id == tabId } ?: return null
        return tab.windows.find { it.id == windowId }
    }

    private suspend fun waitUntilWindowLoaded(tabId: String, windowId: String, expectedLogCount: Int) {
        waitUntil {
            val window = windowState(tabId, windowId) ?: return@waitUntil false
            !window.isLoading && window.logs.size == expectedLogCount
        }
    }

    private suspend fun waitUntilFilterApplied(
        tabId: String,
        windowId: String,
        expectedQueries: List<String>,
        expectedFilteredCount: Int
    ) {
        waitUntil {
            val window = windowState(tabId, windowId) ?: return@waitUntil false
            window.filterQueries == expectedQueries && window.filteredLogs.size == expectedFilteredCount
        }
    }

    private suspend fun waitUntilActiveTab(tabId: String) {
        waitUntil { viewModel.state.value.activeTabId == tabId }
    }

    private suspend fun waitUntil(
        timeoutMillis: Long = 10_000,
        pollIntervalMillis: Long = 20,
        predicate: () -> Boolean
    ) {
        try {
            withTimeout(timeoutMillis.milliseconds) {
                while (!predicate()) {
                    delay(pollIntervalMillis.milliseconds)
                }
            }
        } catch (_: TimeoutCancellationException) {
            throw AssertionError("Condition was not met in time")
        }
    }
}
