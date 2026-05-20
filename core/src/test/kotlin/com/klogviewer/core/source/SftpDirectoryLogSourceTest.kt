package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class SftpDirectoryLogSourceTest {

    @Test
    fun `should detect new files in remote directory and append logs`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/var/log")
        val remoteFileSystem = mockk<RemoteFileSystem>()
        val sshClientProvider = mockk<SshClientProvider>()
        val mockClient = mockk<net.schmizz.sshj.SSHClient>(relaxed = true)
        coEvery { sshClientProvider.createClient() } returns mockClient
        io.mockk.every { mockClient.isConnected } returns true
        io.mockk.every { mockClient.isAuthenticated } returns true
        
        val initialFiles = listOf(RemoteFile("file1.log", "/var/log/file1.log", false))
        val updatedFiles = listOf(
            RemoteFile("file1.log", "/var/log/file1.log", false),
            RemoteFile("file2.log", "/var/log/file2.log", false)
        )
        
        var currentDiscoveredFiles = initialFiles
        coEvery { remoteFileSystem.listFiles(config, "/var/log") } answers { currentDiscoveredFiles.right() }
        
        val entry1 = LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("line1"), sourceId = "sftp://user@remote:22/var/log/file1.log")
        val entry2 = LogEntry(LogTimestamp("2023-01-01 10:01:00"), LogLevel.INFO, LogContent("line2"), sourceId = "sftp://user@remote:22/var/log/file2.log")

        val logSourceFactory: (SftpConfig, net.schmizz.sshj.SSHClient?) -> LogSource = { _, _ ->
            object : LogSource {
                override fun observeLogs(path: LogFilePath, parser: LogParser?) = flow {
                    if (path.value.contains("file1")) {
                        emit(LogUpdate.Initial(listOf(entry1)).right())
                    } else {
                        emit(LogUpdate.Initial(listOf(entry2)).right())
                    }
                    kotlinx.coroutines.delay(Long.MAX_VALUE)
                }
            }
        }

        val source = SftpDirectoryLogSource(
            config, 
            remoteFileSystem, 
            rescanIntervalMs = 100, 
            dispatcher = Dispatchers.Default,
            sshClientProvider = sshClientProvider,
            logSourceFactory = logSourceFactory
        )

        val results = CopyOnWriteArrayList<Either<LogFailure, LogUpdate>>()
        val job = launch {
            source.observeLogs(LogFilePath("/var/log")).collect {
                results.add(it)
            }
        }
        
        // 1. Initial scan
        withTimeout(2000.milliseconds) {
            while (results.isEmpty()) delay(50.milliseconds)
        }
        
        expectThat(results).hasSize(1)
        expectThat(results[0].getOrNull()).isA<LogUpdate.Initial>()
        
        // 2. Update discovered files
        currentDiscoveredFiles = updatedFiles
        
        // 3. Trigger second scan and wait for results
        withTimeout(2000.milliseconds) {
            while (results.size < 2) delay(50.milliseconds)
        }
        
        expectThat(results).hasSize(2)
        val secondUpdate = results[1].getOrNull()
        expectThat(secondUpdate).isA<LogUpdate.Appended>()
        
        job.cancelAndJoin()
    }

    @Test
    fun `should initialize even if some files fail to load`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/var/log")
        val remoteFileSystem = mockk<RemoteFileSystem>()
        val sshClientProvider = mockk<SshClientProvider>()
        val mockClient = mockk<net.schmizz.sshj.SSHClient>(relaxed = true)
        coEvery { sshClientProvider.createClient() } returns mockClient
        io.mockk.every { mockClient.isConnected } returns true
        io.mockk.every { mockClient.isAuthenticated } returns true
        
        val remoteFiles = listOf(
            RemoteFile("good.log", "/var/log/good.log", false),
            RemoteFile("bad.log", "/var/log/bad.log", false)
        )
        
        coEvery { remoteFileSystem.listFiles(config, "/var/log") } returns remoteFiles.right()
        
        val entry = LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("good"), sourceId = "sftp://user@remote:22/var/log/good.log")

        val logSourceFactory: (SftpConfig, net.schmizz.sshj.SSHClient?) -> LogSource = { _, _ ->
            object : LogSource {
                override fun observeLogs(path: LogFilePath, parser: LogParser?) = flow {
                    if (path.value.contains("good")) {
                        emit(LogUpdate.Initial(listOf(entry)).right())
                    } else {
                        emit(LogFailure.FileError("Access denied", sourceId = path.value).left())
                    }
                    kotlinx.coroutines.delay(Long.MAX_VALUE)
                }
            }
        }

        val source = SftpDirectoryLogSource(
            config, 
            remoteFileSystem, 
            rescanIntervalMs = 100, 
            dispatcher = Dispatchers.Default,
            sshClientProvider = sshClientProvider,
            logSourceFactory = logSourceFactory
        )

        val results = CopyOnWriteArrayList<Either<LogFailure, LogUpdate>>()
        val job = launch {
            source.observeLogs(LogFilePath("/var/log")).collect {
                results.add(it)
            }
        }
        
        // Wait for results
        withTimeout(2000.milliseconds) {
            // We expect one LogUpdate.Initial (failures are now suppressed for directory monitoring)
            while (results.size < 1) delay(50.milliseconds)
        }
        
        expectThat(results.mapNotNull { it.getOrNull() }).hasSize(1)
        expectThat(results.mapNotNull { it.getOrNull() }[0]).isA<LogUpdate.Initial>()
        expectThat(results.mapNotNull { it.leftOrNull() }).hasSize(0)
        
        job.cancelAndJoin()
    }

    @Test
    fun `should preserve Appended updates that arrive before directory initialization`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/var/log")
        val remoteFileSystem = mockk<RemoteFileSystem>()
        val sshClientProvider = mockk<SshClientProvider>()
        val mockClient = mockk<net.schmizz.sshj.SSHClient>(relaxed = true)
        coEvery { sshClientProvider.createClient() } returns mockClient
        io.mockk.every { mockClient.isConnected } returns true
        io.mockk.every { mockClient.isAuthenticated } returns true
        
        val remoteFiles = listOf(
            RemoteFile("file1.log", "/var/log/file1.log", false),
            RemoteFile("file2.log", "/var/log/file2.log", false)
        )
        
        coEvery { remoteFileSystem.listFiles(config, "/var/log") } returns remoteFiles.right()
        
        val entry1 = LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("initial1"), sourceId = "sftp://user@remote:22/var/log/file1.log")
        val entry2 = LogEntry(LogTimestamp("2023-01-01 10:01:00"), LogLevel.INFO, LogContent("appended1"), sourceId = "sftp://user@remote:22/var/log/file1.log")
        val entry3 = LogEntry(LogTimestamp("2023-01-01 10:02:00"), LogLevel.INFO, LogContent("initial2"), sourceId = "sftp://user@remote:22/var/log/file2.log")

        val logSourceFactory: (SftpConfig, net.schmizz.sshj.SSHClient?) -> LogSource = { _, _ ->
            object : LogSource {
                override fun observeLogs(path: LogFilePath, parser: LogParser?) = flow {
                    if (path.value.contains("file1")) {
                        emit(LogUpdate.Initial(listOf(entry1)).right())
                        delay(50)
                        emit(LogUpdate.Appended(listOf(entry2)).right())
                    } else {
                        delay(200) // Delay file2 to ensure file1 has time to send Appended
                        emit(LogUpdate.Initial(listOf(entry3)).right())
                    }
                    kotlinx.coroutines.delay(Long.MAX_VALUE)
                }
            }
        }

        val source = SftpDirectoryLogSource(
            config, 
            remoteFileSystem, 
            rescanIntervalMs = 500, 
            dispatcher = Dispatchers.Default,
            sshClientProvider = sshClientProvider,
            logSourceFactory = logSourceFactory
        )

        val results = CopyOnWriteArrayList<Either<LogFailure, LogUpdate>>()
        val job = launch {
            source.observeLogs(LogFilePath("/var/log")).collect {
                results.add(it)
            }
        }
        
        // Wait for Initial
        withTimeout(5000.milliseconds) {
            while (results.none { it.getOrNull() is LogUpdate.Initial }) delay(50.milliseconds)
        }
        
        val initialUpdate = results.first { it.getOrNull() is LogUpdate.Initial }.getOrNull() as LogUpdate.Initial
        
        expectThat(initialUpdate.entries).hasSize(3)
        
        job.cancelAndJoin()
    }
}
