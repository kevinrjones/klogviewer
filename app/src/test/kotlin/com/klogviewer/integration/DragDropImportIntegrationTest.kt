package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.ui.mvi.KLogViewerEvent
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class DragDropImportIntegrationTest {
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
    fun `dropping one or more files onto log view adds them to current tab`() = runBlocking {
        val file1 = createLogFile("log-drop-current-1", "2024-01-01 10:00:00 [INFO] current one")
        val file2 = createLogFile("log-drop-current-2", "2024-01-01 10:00:01 [INFO] current two")

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnLogView(listOf(file1.absolutePath)))
        waitForSourceCount(expected = 1)

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnLogView(listOf(file2.absolutePath)))
        waitForSourceCount(expected = 2)

        val sourceIds = viewModel.state.value.activeTab!!.activeWindow!!.sourceIds
        assertEquals(listOf(file1.absolutePath, file2.absolutePath), sourceIds)
    }

    @Test
    fun `dropping files while tab shows welcome page loads files into the same tab window`() = runBlocking {
        val initialTabId = viewModel.state.value.activeTabId
        val initialWindow = viewModel.state.value.activeTab!!.activeWindow!!
        assertTrue(initialWindow.filePath.isEmpty())
        assertTrue(initialWindow.logs.isEmpty())
        assertTrue(!initialWindow.isDirectory)

        val file = createLogFile("log-drop-welcome", "2024-01-01 10:30:00 [INFO] welcome drop")

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnLogView(listOf(file.absolutePath)))

        withTimeout(3000.milliseconds) {
            viewModel.state.first {
                val activeTab = it.activeTab
                val activeWindow = activeTab?.activeWindow
                it.activeTabId == initialTabId &&
                    activeWindow != null &&
                    !activeWindow.isLoading &&
                    activeWindow.sourceIds == listOf(file.absolutePath)
            }
        }
    }

    @Test
    fun `dropping one or more files onto tab bar creates a new tab and loads files there`() = runBlocking {
        val originalTabId = viewModel.state.value.activeTabId
        val file1 = createLogFile("log-drop-tab-1", "2024-01-01 11:00:00 [INFO] tab one")
        val file2 = createLogFile("log-drop-tab-2", "2024-01-01 11:00:01 [INFO] tab two")

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnTabBar(listOf(file1.absolutePath, file2.absolutePath)))

        withTimeout(3000.milliseconds) {
            viewModel.state.first {
                val activeWindow = it.activeTab?.activeWindow
                it.activeTabId != originalTabId &&
                    activeWindow != null &&
                    !activeWindow.isLoading &&
                    activeWindow.sourceIds.containsAll(listOf(file1.absolutePath, file2.absolutePath))
            }
        }

        val state = viewModel.state.value
        assertEquals(2, state.tabs.size)
        assertTrue(state.activeTabId != originalTabId)
        assertEquals(listOf(file1.absolutePath, file2.absolutePath), state.activeTab!!.activeWindow!!.sourceIds)
    }

    @Test
    fun `invalid dropped items are ignored and reported with non-blocking feedback`() = runBlocking {
        val invalidPath = File(tempDir, "missing-drop.log").absolutePath

        val infoEventDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3000.milliseconds) {
                viewModel.events.first { it is KLogViewerEvent.ShowInfo } as KLogViewerEvent.ShowInfo
            }
        }

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnLogView(listOf(invalidPath)))

        val event = infoEventDeferred.await()

        assertEquals(
            "Dropped items are not supported.",
            event.message
        )
        assertTrue(viewModel.state.value.activeTab?.activeWindow?.sourceIds?.isEmpty() == true)
    }

    @Test
    fun `mixed valid and invalid multi-file drops load valid files and report invalid ones`() = runBlocking {
        val valid = createLogFile("log-drop-mixed-valid", "2024-01-01 12:00:00 [INFO] mixed")
        val invalid = File(tempDir, "missing-mixed.log").absolutePath

        val infoEventDeferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(3000.milliseconds) {
                viewModel.events.first { it is KLogViewerEvent.ShowInfo } as KLogViewerEvent.ShowInfo
            }
        }

        viewModel.handleIntent(KLogViewerIntent.DropFilesOnLogView(listOf(valid.absolutePath, invalid)))
        waitForSourceCount(expected = 1)

        val infoEvent = infoEventDeferred.await()

        assertEquals("Ignored 1 unsupported dropped item(s).", infoEvent.message)
        assertEquals(listOf(valid.absolutePath), viewModel.state.value.activeTab!!.activeWindow!!.sourceIds)
    }

    private fun createLogFile(prefix: String, line: String): File {
        return File.createTempFile(prefix, ".log", tempDir).apply {
            writeText("$line\n")
            deleteOnExit()
        }
    }

    private suspend fun waitForSourceCount(expected: Int) {
        withTimeout(3000.milliseconds) {
            viewModel.state.first {
                val activeWindow = it.activeTab?.activeWindow
                activeWindow != null && !activeWindow.isLoading && activeWindow.sourceIds.size == expected
            }
        }
    }
}