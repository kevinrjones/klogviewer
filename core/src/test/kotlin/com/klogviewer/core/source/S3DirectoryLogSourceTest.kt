package com.klogviewer.core.source

import arrow.core.left
import arrow.core.right
import com.klogviewer.domain.model.*
import com.klogviewer.domain.parser.LogParser
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.RemoteFileSystem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import kotlin.time.Duration.Companion.milliseconds

class S3DirectoryLogSourceTest {

    @Suppress("JUnitMalformedDeclaration")
    @Test
    fun `should restart object observation when previous observer completed after timeout failure`() = runBlocking {
        val config = S3Config("test", "my-bucket", "us-east-1", S3Auth.DefaultChain, "/logs")
        val remoteFileSystem = mockk<RemoteFileSystem>()

        val objects = listOf(RemoteFile("app.log", "logs/app.log", false))
        coEvery { remoteFileSystem.listS3Objects(config, "/logs/") } returns objects.right()

        val recoveredEntry = LogEntry(
            LogTimestamp("2023-01-01 10:05:00"),
            LogLevel.INFO,
            LogContent("s3 recovered after timeout"),
            sourceId = "s3://my-bucket/logs/app.log"
        )

        var observeAttempts = 0
        val logSourceFactory: (S3Config) -> LogSource = {
            object : LogSource {
                override fun observeLogs(path: LogFilePath, parser: LogParser?) = flow {
                    observeAttempts += 1
                    if (observeAttempts == 1) {
                        emit(LogFailure.FileError("Request timed out", sourceId = null).left())
                        return@flow
                    }

                    emit(LogUpdate.Initial(listOf(recoveredEntry)).right())
                    delay(Long.MAX_VALUE.milliseconds)
                }
            }
        }

        val source = S3DirectoryLogSource(
            config,
            remoteFileSystem,
            rescanIntervalMs = 100,
            dispatcher = Dispatchers.Default,
            logSourceFactory = logSourceFactory
        )

        val results = withTimeout(3000.milliseconds) {
            source.observeLogs(LogFilePath("/logs")).take(2).toList()
        }

        expectThat(results).hasSize(2)

        val initial = results[0].getOrNull()
        expectThat(initial).isA<LogUpdate.Initial>()
        expectThat((initial as LogUpdate.Initial).entries).hasSize(0)

        val recovery = results[1].getOrNull()
        expectThat(recovery).isA<LogUpdate.Appended>()
        expectThat((recovery as LogUpdate.Appended).entries).hasSize(1)
        expectThat(recovery.entries[0].content).isEqualTo(LogContent("s3 recovered after timeout"))
    }
}
