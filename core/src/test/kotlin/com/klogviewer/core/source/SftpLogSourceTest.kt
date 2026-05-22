package com.klogviewer.core.source

import arrow.core.Either
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.domain.model.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SftpLogSourceTest {

    private class NonInterruptibleBlockingInputStream : InputStream() {
        private val readStarted = CompletableDeferred<Unit>()

        @Volatile
        private var closed = false

        suspend fun awaitReadStarted() {
            readStarted.await()
        }

        override fun available(): Int = 1

        override fun read(): Int {
            if (!readStarted.isCompleted) {
                readStarted.complete(Unit)
            }

            while (!closed) {
                try {
                    Thread.sleep(25)
                } catch (_: InterruptedException) {
                    // Intentionally ignored to simulate a non-cooperative blocking read.
                }
            }

            return -1
        }

        override fun close() {
            closed = true
        }
    }

    @Test
    fun `should connect, auth and tail logs using mocked SSH client`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/var/log/test.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        val pipedOut = PipedOutputStream()
        val pipedIn = PipedInputStream(pipedOut)
        
        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.exitStatus } returns null
        
        val initialReceived = CompletableDeferred<Unit>()
        
        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act & Assert
        // Write Initial logs BEFORE observing to ensure they are picked up in the Initial update
        pipedOut.write("2026-05-20 08:00:00 INFO Initial\n".toByteArray())
        pipedOut.flush()

        launch {
            // Wait for initial load to be completed by the observer
            initialReceived.await()
            
            // Now write appended logs
            pipedOut.write("2026-05-20 08:00:01 INFO Appended\n".toByteArray())
            pipedOut.flush()
            
            delay(100.milliseconds) // Give it time to be read
            pipedOut.close() // Close pipe to end the flow
            
            // After close, make exitStatus return 0
            every { mockCommand.exitStatus } returns 0
        }

        val results = source.observeLogs(LogFilePath("/var/log/test.log"))
            .onEach { 
                if (it.getOrNull() is LogUpdate.Initial) {
                    initialReceived.complete(Unit) 
                }
            }
            .take(2)
            .toList()

        // Assert
        expectThat(results).hasSize(2)
        
        val initialResult = results[0].getOrNull() as LogUpdate.Initial
        expectThat(initialResult.entries).hasSize(1)
        expectThat(initialResult.entries[0].content).isEqualTo(LogContent("Initial"))
        
        val appendedResult = results[1].getOrNull() as LogUpdate.Appended
        expectThat(appendedResult.entries).hasSize(1)
        expectThat(appendedResult.entries[0].content).isEqualTo(LogContent("Appended"))

        verify { mockClient.connect("remote", 22) }
        verify { mockClient.authPassword("user", "pass") }
        verify { mockSession.exec(match { it.contains("tail -n +1 -f") }) }
    }

    @Test
    fun `should support key-based authentication`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.KeyPair("/path/to/key"), "/test.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockKeyProvider = mockk<KeyProvider>()
        
        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.loadKeys(any<String>()) } returns mockKeyProvider
        
        // Use a closed stream to immediately end the flow
        val pipedIn = PipedInputStream()
        PipedOutputStream(pipedIn).close()
        
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.exitStatus } returns null

        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act
        source.observeLogs(LogFilePath("/test.log")).take(1).toList()

        // Assert
        verify { mockClient.loadKeys("/path/to/key") }
        verify { mockClient.authPublickey("user", mockKeyProvider) }
    }

    @Test
    fun `should support key-based authentication with passphrase`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.KeyPair("/path/to/key", "secret"), "/test.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockKeyProvider = mockk<KeyProvider>()
        
        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.loadKeys(any<String>(), any<String>()) } returns mockKeyProvider
        
        // Use a closed stream to immediately end the flow
        val pipedIn = PipedInputStream()
        PipedOutputStream(pipedIn).close()
        
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.exitStatus } returns null

        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act
        source.observeLogs(LogFilePath("/test.log")).take(1).toList()

        // Assert
        verify { mockClient.loadKeys("/path/to/key", "secret") }
        verify { mockClient.authPublickey("user", mockKeyProvider) }
    }

    @Test
    fun `should support key-based authentication with blank passphrase`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.KeyPair("/path/to/key", "  "), "/test.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockKeyProvider = mockk<KeyProvider>()
        
        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.loadKeys(any<String>()) } returns mockKeyProvider
        
        // Use a closed stream to immediately end the flow
        val pipedIn = PipedInputStream()
        PipedOutputStream(pipedIn).close()
        
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.exitStatus } returns null

        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act
        source.observeLogs(LogFilePath("/test.log")).take(1).toList()

        // Assert
        verify { mockClient.loadKeys("/path/to/key") }
    }

    @Test
    fun `should emit failure if remote file does not exist`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/invalid/path")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        
        // Empty input stream (closed immediately)
        val pipedIn = PipedInputStream()
        PipedOutputStream(pipedIn).close()
        
        // Error stream with error message
        val errorIn = PipedInputStream()
        val errorOut = PipedOutputStream(errorIn)
        errorOut.write("tail: cannot open '/invalid/path' for reading: No such file or directory\n".toByteArray())
        errorOut.close()

        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.errorStream } returns errorIn
        every { mockCommand.exitStatus } returns 1
        
        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act
        val results = source.observeLogs(LogFilePath("/invalid/path")).toList()

        // Assert
        expectThat(results.any { it.isLeft() }).isEqualTo(true)
        val failureResult = results.first { it.isLeft() }
        val failure = (failureResult as Either.Left).value
        expectThat(failure).isA<LogFailure.FileError>()
        Unit
    }

    @Test
    fun `should emit empty initial load for empty remote file`() = runBlocking {
        // Arrange
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/empty.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        
        // Input stream that stays open but has no data
        val pipedIn = PipedInputStream()
        // We don't close it yet

        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }
        
        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns pipedIn
        every { mockCommand.exitStatus } returns null // Still running
        
        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)

        // Act
        val results = source.observeLogs(LogFilePath("/empty.log")).take(1).toList()

        // Assert
        expectThat(results).hasSize(1)
        expectThat(results[0].isRight()).isEqualTo(true)
        val update = (results[0] as Either.Right).value
        expectThat(update).isA<LogUpdate.Initial>()
        expectThat((update as LogUpdate.Initial).entries).hasSize(0)
        Unit
    }

    @Test
    fun `should cancel promptly when remote read is blocking`() = runBlocking {
        val config = SftpConfig("test", Host("remote"), Port(22), Username("user"), SftpAuth.Password("pass"), "/blocking.log")
        val mockClient = mockk<SSHClient>(relaxed = true)
        val mockSession = mockk<Session>(relaxed = true)
        val mockCommand = mockk<Session.Command>(relaxed = true)
        val blockingInput = NonInterruptibleBlockingInputStream()

        val provider = object : SshClientProvider {
            override fun createClient(): SSHClient = mockClient
        }

        every { mockClient.startSession() } returns mockSession
        every { mockSession.exec(any()) } returns mockCommand
        every { mockCommand.inputStream } returns blockingInput
        every { mockCommand.errorStream } returns ByteArrayInputStream(byteArrayOf())
        every { mockCommand.exitStatus } returns null
        every { mockCommand.close() } answers {
            blockingInput.close()
            Unit
        }

        val source = SftpLogSource(config, SimpleLogParser(), provider, Dispatchers.IO)
        val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        try {
            val observerJob = observationScope.launch {
                source.observeLogs(LogFilePath("/blocking.log")).collect { }
            }

            blockingInput.awaitReadStarted()

            withTimeout(1.seconds) {
                observerJob.cancelAndJoin()
            }

            verify(atLeast = 1) { mockCommand.close() }
        } finally {
            blockingInput.close()
            observationScope.cancel()
        }
    }
}
