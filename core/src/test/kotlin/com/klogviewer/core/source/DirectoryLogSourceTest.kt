package com.klogviewer.core.source

import arrow.core.Either
import arrow.core.right
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.domain.model.*
import com.klogviewer.domain.repository.LogSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isA
import java.io.File
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryLogSourceTest {

    @TempDir
    lateinit var tempDir: Path

    private val testDispatcher = StandardTestDispatcher()
    private val fileLogSource = mockk<LogSource>()
    private val registry = ParserRegistry()
    private val heuristicProbe = HeuristicProbe(registry)
    private val scanner = DirectoryScanner()

    @Test
    fun `should merge initial logs from multiple files in directory`() = runTest(testDispatcher) {
        val log1 = File(tempDir.toFile(), "app.log").apply { writeText("2023-01-01 10:00:00 INFO log1") }
        val log2 = File(tempDir.toFile(), "error.log").apply { writeText("2023-01-01 10:01:00 ERROR log2") }
        
        val entry1 = LogEntry(LogTimestamp("2023-01-01 10:00:00"), LogLevel.INFO, LogContent("log1"), sourceId = log1.absolutePath)
        val entry2 = LogEntry(LogTimestamp("2023-01-01 10:01:00"), LogLevel.ERROR, LogContent("log2"), sourceId = log2.absolutePath)

        every { fileLogSource.observeLogs(LogFilePath(log1.absolutePath), any()) } returns flowOf(LogUpdate.Initial(listOf(entry1)).right())
        every { fileLogSource.observeLogs(LogFilePath(log2.absolutePath), any()) } returns flowOf(LogUpdate.Initial(listOf(entry2)).right())

        val source = DirectoryLogSource(fileLogSource, heuristicProbe, scanner, rescanIntervalMs = 100, dispatcher = testDispatcher)
        
        val results = mutableListOf<Either<LogFailure, LogUpdate>>()
        val job = launch {
            source.observeLogs(LogFilePath(tempDir.toFile().absolutePath)).collect {
                results.add(it)
            }
        }
        
        testScheduler.advanceTimeBy(200)
        
        expectThat(results).hasSize(1)
        val initial = results[0].getOrNull()
        expectThat(initial).isA<LogUpdate.Initial>()
        val entries = (initial as LogUpdate.Initial).entries
        expectThat(entries).hasSize(2)
        
        job.cancel()
    }
}
