package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.JsonLogParser
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class WorkspaceLogLoaderTest {

    private val localFileSystem = mockk<LocalFileSystem>()
    private val remoteFileSystem = mockk<RemoteFileSystem>()
    private val logSource = mockk<LogSource>(relaxed = true)
    private val heuristicProbe = mockk<HeuristicProbe>()
    private val logSourceFactory = mockk<LogSourceFactory>(relaxed = true)
    private val state = MutableStateFlow(KLogViewerState())

    @Test
    fun `given parser override when auto detection would choose json then override remains authoritative`() {
        val path = "/tmp/test.log"
        val sampleLines = listOf(
            """{"timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"detected json"}"""
        )

        every { localFileSystem.exists(path) } returns true
        every { localFileSystem.isDirectory(path) } returns false
        every { localFileSystem.readLines(path, any()) } returns sampleLines
        every { heuristicProbe.detect(any()) } returns ProbeResult(
            parser = JsonLogParser(),
            parserName = "JSON",
            columns = listOf("Timestamp", "Level", "Content")
        )

        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = heuristicProbe,
            logSourceFactory = logSourceFactory,
            state = state
        )

        val results = loader.performHeuristicDetection(
            paths = listOf(path),
            overrideParserName = "Simple"
        )

        val selected = results.single()
        expectThat(selected).isNotNull()
        expectThat(selected?.parserName).isEqualTo("Simple")
        verify(exactly = 0) { heuristicProbe.detect(any()) }
    }

    @Test
    fun `given json parser override when json is detected then override stays json`() {
        val path = "/tmp/structured.log"
        val sampleLines = listOf(
            """{"timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"structured"}"""
        )

        every { localFileSystem.exists(path) } returns true
        every { localFileSystem.isDirectory(path) } returns false
        every { localFileSystem.readLines(path, any()) } returns sampleLines
        every { heuristicProbe.detect(any()) } returns ProbeResult(
            parser = JsonLogParser(),
            parserName = "JSON",
            columns = listOf("Timestamp", "Level", "Content")
        )

        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = heuristicProbe,
            logSourceFactory = logSourceFactory,
            state = state
        )

        val result = loader.performHeuristicDetection(
            paths = listOf(path),
            overrideParserName = "JSON"
        ).single()

        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("JSON")
        verify(exactly = 1) { heuristicProbe.detect(sampleLines) }
    }

    @Test
    fun `given malformed json-like lines when repeatedly detected then selection is deterministic`() {
        val probe = HeuristicProbe(ParserRegistry())
        val lines = listOf(
            "{partial",
            "random text",
            "{"
        )

        val selections = (1..20).map { probe.detect(lines).parserName }.toSet()

        expectThat(selections.size).isEqualTo(1)
    }
}
