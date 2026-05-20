package com.klogviewer.integration

import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SftpBrowsingTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { PreferencesRepository(tempDir) }
    private val remoteFileSystem = mockk<RemoteFileSystem>()
    private val mockSftpSource = mockk<LogSource>(relaxed = true)
    private val viewModel by lazy { 
        KLogViewerViewModel(
            source, 
            prefsRepository, 
            heuristicProbe, 
            remoteFileSystem = remoteFileSystem,
            sftpSourceFactory = { _, _ -> mockSftpSource }
        ) 
    }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.clear()
    }

    @Test
    fun `should transition to SFTP_BROWSE when connecting to a directory`() = runBlocking {
        // Arrange
        val name = "Server"
        val host = "example.com"
        val auth = SftpAuth.Password("pass")
        val path = "/var/log"
        
        val remoteFiles = listOf(
            RemoteFile("file1.log", "/var/log/file1.log", false),
            RemoteFile("file2.log", "/var/log/file2.log", false)
        )
        
        coEvery { remoteFileSystem.listFiles(any(), path) } returns remoteFiles.right()
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectSftp(name, host, 22, "user", auth, path))
        
        // Assert
        // Need a small delay because ConnectSftp launches a coroutine for listFiles
        kotlinx.coroutines.delay(100.milliseconds)
        
        val state = viewModel.state.value
        assertEquals(KLogViewerState.DialogType.SFTP_BROWSE, state.pendingDialog)
        assertEquals(2, state.remoteFiles.size)
        assertEquals(path, state.remoteBrowsePath)
    }

    @Test
    fun `should connect directly when connecting to a file`() = runBlocking {
        // Arrange
        val name = "Server"
        val host = "example.com"
        val auth = SftpAuth.Password("pass")
        val path = "/var/log/syslog"
        
        // ls of a file usually returns the file itself
        val remoteFiles = listOf(
            RemoteFile("syslog", "/var/log/syslog", false)
        )
        
        coEvery { remoteFileSystem.listFiles(any(), path) } returns remoteFiles.right()
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectSftp(name, host, 22, "user", auth, path))
        
        // Assert
        kotlinx.coroutines.delay(100)
        
        val state = viewModel.state.value
        assertEquals(null, state.pendingDialog)
        // Verify window is loading the file
        val window = state.activeTab?.activeWindow
        assertEquals("sftp://user@example.com:22/var/log/syslog", window?.filePath)
    }
}
