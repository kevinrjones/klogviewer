package com.klogviewer.integration

import arrow.core.left
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SftpReloadTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should mark SFTP source as missing if connection fails on startup`() = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val prefsRepo = JsonPreferencesRepository(prefsDir)
        
        val sftpConfig = SftpConfig(
            name = "My Server",
            host = Host("1.2.3.4"),
            port = Port(22),
            username = Username("user"),
            auth = SftpAuth.Password("pass")
        )
        
        val sftpUri = "sftp://user@1.2.3.4:22/var/log/syslog"
        
        val initialPrefs = UserPreferences(
            sftpConnections = listOf(sftpConfig),
            tabs = listOf(
                TabPreference(
                    id = "tab-1",
                    title = "SFTP Tab",
                    activeWindowId = "win-1",
                    windows = listOf(
                        WindowPreference(
                            id = "win-1",
                            filePath = sftpUri,
                            sourceIds = listOf(sftpUri)
                        )
                    )
                )
            ),
            activeTabId = "tab-1"
        )
        prefsRepo.save(initialPrefs)
        
        val remoteFileSystem = mockk<RemoteFileSystem>()
        coEvery { remoteFileSystem.isSftpDirectory(any(), any()) } returns false
        val mockSftpSource = mockk<LogSource>()
        
        val sftpSourceResult = LogFailure.FileError("Connection failed", sourceId = sftpUri).left()
        every { mockSftpSource.observeLogs(any(), any()) } returns kotlinx.coroutines.flow.flowOf(sftpSourceResult)
        
        val logSourceFactory = mockk<com.klogviewer.domain.repository.LogSourceFactory>()
        every { logSourceFactory.createSftpSource(any(), any()) } returns mockSftpSource
        every { logSourceFactory.createSftpDirectorySource(any(), any()) } returns mockSftpSource

        // Use a real ViewModel but with mocked sources
        val viewModel = KLogViewerViewModel(
            logSource = mockk(),
            prefsRepository = prefsRepo,
            heuristicProbe = HeuristicProbe(ParserRegistry()),
            remoteFileSystem = remoteFileSystem,
            logSourceFactory = logSourceFactory
        )
        
        // Wait for it to fail and mark as missing
        withTimeout(5.seconds) {
            viewModel.state.first { state ->
                val window = state.activeTab?.activeWindow
                window != null && window.missingSourceIds.contains(sftpUri)
            }
        }
        
        val window = viewModel.state.value.activeTab?.activeWindow!!
        expectThat(window.missingSourceIds).contains(sftpUri)
        expectThat(window.error).isNotNull()
        
        viewModel.clear()
    }
}
