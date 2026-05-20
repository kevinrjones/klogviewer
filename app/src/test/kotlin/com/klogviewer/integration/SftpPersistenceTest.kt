package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SftpPersistenceTest {
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
    fun `should automatically save SFTP connection details when connecting`() = runBlocking {
        // Arrange
        val name = "Prod Server"
        val host = "example.com"
        val port = 22
        val user = "user"
        val auth = SftpAuth.Password("pass")
        val path = "/var/log/remote.log"
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectSftp(name, host, port, user, auth, path))
        
        // Assert
        val connections = viewModel.state.value.sftpConnections
        assertEquals(1, connections.size)
        assertEquals(name, connections[0].name)
        assertEquals(host, connections[0].host.value)
        assertEquals(path, connections[0].logFilePath)
        
        // Verify it persists in repository
        val savedPrefs = prefsRepository.load()
        assertEquals(1, savedPrefs.sftpConnections.size)
        assertEquals(name, savedPrefs.sftpConnections[0].name)
    }

    @Test
    fun `should override existing SFTP connection with same name when connecting`() = runBlocking {
        // Arrange
        val name = "Existing"
        val initialConfig = SftpConfig(name, Host("old.com"), Port(22), Username("old"), SftpAuth.Password("old"), "/old.log")
        viewModel.handleIntent(KLogViewerIntent.SaveSftpConnection(initialConfig))
        
        val newHost = "new.com"
        val newAuth = SftpAuth.Password("new")
        val newPath = "/new.log"
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectSftp(name, newHost, 22, "user", newAuth, newPath))
        
        // Assert
        val connections = viewModel.state.value.sftpConnections
        assertEquals(1, connections.size)
        assertEquals(name, connections[0].name)
        assertEquals(newHost, connections[0].host.value)
        assertEquals(newPath, connections[0].logFilePath)
    }
}
