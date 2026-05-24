package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.*
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class S3PersistenceTest {
    @TempDir
    lateinit var tempDir: File

    private val parser = SimpleLogParser()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val source = FileLogSource(parser)
    private val prefsRepository by lazy { JsonPreferencesRepository(tempDir) }
    private val logSourceFactory = io.mockk.mockk<com.klogviewer.domain.repository.LogSourceFactory>(relaxed = true)
    private val viewModel by lazy { 
        KLogViewerViewModel(
            logSource = source, 
            prefsRepository = prefsRepository, 
            heuristicProbe = heuristicProbe,
            logSourceFactory = logSourceFactory
        ) 
    }

    @AfterEach
    fun tearDown() {
        viewModel.clear()
    }

    @Test
    fun `should automatically save S3 connection details when connecting`() = runBlocking {
        // Arrange
        val name = "Prod S3"
        val bucket = "prod-logs"
        val region = "us-west-2"
        val auth = S3Auth.DefaultChain
        val prefix = "app.log"
        val config = S3Config(name, bucket, region, auth, prefix)
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectS3(config))
        
        // Assert
        val connections = viewModel.state.value.s3Connections
        assertEquals(1, connections.size)
        assertEquals(name, connections[0].name)
        assertEquals(bucket, connections[0].bucket)
        assertEquals(prefix, connections[0].prefix)
        
        // Verify it persists in repository
        val savedPrefs = prefsRepository.load()
        assertEquals(1, savedPrefs.s3Connections.size)
        assertEquals(name, savedPrefs.s3Connections[0].name)
    }

    @Test
    fun `should override existing S3 connection with same name when connecting`() = runBlocking {
        // Arrange
        val name = "Existing S3"
        val initialConfig = S3Config(name, "old-bucket", "us-east-1", S3Auth.DefaultChain, "old.log")
        viewModel.handleIntent(KLogViewerIntent.SaveS3Connection(initialConfig))
        
        val newBucket = "new-bucket"
        val newPrefix = "new.log"
        val newConfig = S3Config(name, newBucket, "us-east-1", S3Auth.DefaultChain, newPrefix)
        
        // Act
        viewModel.handleIntent(KLogViewerIntent.ConnectS3(newConfig))
        
        // Assert
        val connections = viewModel.state.value.s3Connections
        assertEquals(1, connections.size)
        assertEquals(name, connections[0].name)
        assertEquals(newBucket, connections[0].bucket)
        assertEquals(newPrefix, connections[0].prefix)
    }
}
