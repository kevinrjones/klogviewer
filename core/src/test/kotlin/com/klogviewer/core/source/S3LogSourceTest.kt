package com.klogviewer.core.source

import arrow.core.Either
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectResponse
import aws.sdk.kotlin.services.s3.model.HeadObjectRequest
import aws.sdk.kotlin.services.s3.model.HeadObjectResponse
import aws.smithy.kotlin.runtime.content.ByteStream
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.domain.model.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import kotlin.time.Duration.Companion.milliseconds

class S3LogSourceTest {

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should poll logs from S3 bucket`() = runTest {
        // Arrange
        val config = S3Config("test", "my-bucket", "us-east-1", S3Auth.DefaultChain, "/logs/app.log")
        val mockClient = mockk<S3Client>(relaxed = true)
        val mockProvider = mockk<S3ClientProvider>()
        
        coEvery { mockProvider.createClient(any()) } returns mockClient
        
        val line1 = "2024-05-23 10:00:00 INFO Initial"
        val line2 = "2024-05-23 10:00:01 INFO Appended"
        
        val headResponse1 = HeadObjectResponse { contentLength = line1.length.toLong() + 1 }
        val headResponse2 = HeadObjectResponse { contentLength = (line1.length + line2.length + 2).toLong() }
        
        coEvery { mockClient.headObject(any()) } returns headResponse1 andThen headResponse2 andThen headResponse2
        
        coEvery { mockClient.getObject<Any?>(any(), any()) } coAnswers {
            val req = it.invocation.args[0] as GetObjectRequest
            val block = it.invocation.args[1] as suspend (GetObjectResponse) -> Any?
            
            val content = if ((req.range ?: "").startsWith("bytes=0-")) {
                "$line1\n"
            } else {
                "$line2\n"
            }
            
            block(GetObjectResponse { body = ByteStream.fromString(content) })
        }
        
        val source = S3LogSource(
            config, 
            SimpleLogParser(), 
            s3ClientProvider = mockProvider, 
            pollingInterval = 10.milliseconds,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        // Act
        val results = source.observeLogs(LogFilePath("/logs/app.log")).take(2).toList()

        // Assert
        expectThat(results).hasSize(2)
        
        val initialResult = results[0].getOrNull() as LogUpdate.Initial
        expectThat(initialResult.entries).hasSize(1)
        expectThat(initialResult.entries[0].content).isEqualTo(LogContent("Initial"))
        
        val appendedResult = results[1].getOrNull() as LogUpdate.Appended
        expectThat(appendedResult.entries).hasSize(1)
        expectThat(appendedResult.entries[0].content).isEqualTo(LogContent("Appended"))
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should continue polling after transient headObject failure`() = runTest {
        // Arrange
        val config = S3Config("test", "my-bucket", "us-east-1", S3Auth.DefaultChain, "/logs/app.log")
        val mockClient = mockk<S3Client>(relaxed = true)
        val mockProvider = mockk<S3ClientProvider>()

        val line1 = "2024-05-23 10:00:00 INFO Recovered"
        val headResponse = HeadObjectResponse { contentLength = line1.length.toLong() + 1 }

        coEvery { mockProvider.createClient(any()) } returns mockClient
        coEvery { mockClient.headObject(any<HeadObjectRequest>()) } throws Exception("connection reset") andThen headResponse andThen headResponse
        coEvery { mockClient.getObject<Any?>(any(), any()) } coAnswers {
            val block = it.invocation.args[1] as suspend (GetObjectResponse) -> Any?
            block(GetObjectResponse { body = ByteStream.fromString("$line1\n") })
        }

        val source = S3LogSource(
            config,
            SimpleLogParser(),
            s3ClientProvider = mockProvider,
            pollingInterval = 10.milliseconds,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        // Act
        val results = source.observeLogs(LogFilePath("/logs/app.log")).take(2).toList()

        // Assert
        expectThat(results).hasSize(2)
        expectThat(results[0].isLeft()).isEqualTo(true)
        val failure = (results[0] as Either.Left).value
        expectThat((failure as LogFailure.FileError).message)
            .isEqualTo("S3 object not found or inaccessible: s3://my-bucket/logs/app.log. Error: connection reset")

        val recovered = results[1].getOrNull() as LogUpdate.Initial
        expectThat(recovered.entries).hasSize(1)
        expectThat(recovered.entries[0].content).isEqualTo(LogContent("Recovered"))
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should continue polling after transient getObject failure`() = runTest {
        // Arrange
        val config = S3Config("test", "my-bucket", "us-east-1", S3Auth.DefaultChain, "/logs/app.log")
        val mockClient = mockk<S3Client>(relaxed = true)
        val mockProvider = mockk<S3ClientProvider>()

        val line = "2024-05-23 10:00:00 INFO Recovered"
        val headResponse = HeadObjectResponse { contentLength = line.length.toLong() + 1 }
        var getObjectCallCount = 0

        coEvery { mockProvider.createClient(any()) } returns mockClient
        coEvery { mockClient.headObject(any<HeadObjectRequest>()) } returns headResponse andThen headResponse andThen headResponse
        coEvery { mockClient.getObject<Any?>(any(), any()) } coAnswers {
            getObjectCallCount += 1
            if (getObjectCallCount == 1) {
                throw Exception("socket timeout")
            }

            val block = it.invocation.args[1] as suspend (GetObjectResponse) -> Any?
            block(GetObjectResponse { body = ByteStream.fromString("$line\n") })
        }

        val source = S3LogSource(
            config,
            SimpleLogParser(),
            s3ClientProvider = mockProvider,
            pollingInterval = 10.milliseconds,
            dispatcher = UnconfinedTestDispatcher(testScheduler)
        )

        // Act
        val results = source.observeLogs(LogFilePath("/logs/app.log")).take(2).toList()

        // Assert
        expectThat(results).hasSize(2)
        expectThat(results[0].isLeft()).isEqualTo(true)
        val failure = (results[0] as Either.Left).value as LogFailure.FileError
        expectThat(failure.message).isEqualTo("Error polling S3 object: socket timeout")

        val recovered = results[1].getOrNull() as LogUpdate.Initial
        expectThat(recovered.entries).hasSize(1)
        expectThat(recovered.entries[0].content).isEqualTo(LogContent("Recovered"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `should propagate cancellation exceptions from polling`() {
        val config = S3Config("test", "my-bucket", "us-east-1", S3Auth.DefaultChain, "/logs/app.log")
        val mockClient = mockk<S3Client>(relaxed = true)
        val mockProvider = mockk<S3ClientProvider>()

        coEvery { mockProvider.createClient(any()) } returns mockClient
        coEvery { mockClient.headObject(any<HeadObjectRequest>()) } throws CancellationException("cancelled")

        val source = S3LogSource(
            config,
            SimpleLogParser(),
            s3ClientProvider = mockProvider,
            pollingInterval = 10.milliseconds,
            dispatcher = UnconfinedTestDispatcher()
        )

        assertThrows<CancellationException> {
            runBlocking {
                source.observeLogs(LogFilePath("/logs/app.log")).take(1).toList()
            }
        }
    }
}
