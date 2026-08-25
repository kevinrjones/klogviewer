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

    @Test
    fun `given saved directory mapping when text log opened then saved mapping is used directly`() {
        val path = "/var/log/app.log"
        val sampleLines = listOf("2026-08-25 10:00:00.123 [main] INFO MyService - Hello world")
        val draft = createSampleDraft()
        val directoryKey = "local:/var/log"
        val mapping = com.klogviewer.domain.model.DirectoryPatternMapping(
            directoryKey = directoryKey,
            patternDraft = draft
        )
        val testProbe = HeuristicProbe(ParserRegistry())
        val customState = MutableStateFlow(
            KLogViewerState(directoryPatternMappings = mapOf(directoryKey to mapping))
        )

        every { localFileSystem.exists(path) } returns true
        every { localFileSystem.isDirectory(path) } returns false
        every { localFileSystem.readLines(path, any()) } returns sampleLines

        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = testProbe,
            logSourceFactory = logSourceFactory,
            state = customState
        )

        val result = loader.performHeuristicDetection(paths = listOf(path), overrideParserName = null).single()

        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("Custom Saved Logback")
    }

    private fun createSampleDraft(): com.klogviewer.domain.model.PatternDraft {
        return com.klogviewer.domain.model.PatternDraft(
            name = "Custom Saved Logback",
            segments = listOf(
                com.klogviewer.domain.model.PatternSegment.Token(
                    com.klogviewer.domain.model.PatternToken(
                        role = com.klogviewer.domain.model.PatternTokenRole.TIMESTAMP,
                        formatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                    )
                ),
                com.klogviewer.domain.model.PatternSegment.Delimiter(
                    com.klogviewer.domain.model.PatternDelimiter(value = " [")
                ),
                com.klogviewer.domain.model.PatternSegment.Token(
                    com.klogviewer.domain.model.PatternToken(role = com.klogviewer.domain.model.PatternTokenRole.THREAD)
                ),
                com.klogviewer.domain.model.PatternSegment.Delimiter(
                    com.klogviewer.domain.model.PatternDelimiter(value = "] ")
                ),
                com.klogviewer.domain.model.PatternSegment.Token(
                    com.klogviewer.domain.model.PatternToken(role = com.klogviewer.domain.model.PatternTokenRole.LEVEL)
                ),
                com.klogviewer.domain.model.PatternSegment.Delimiter(
                    com.klogviewer.domain.model.PatternDelimiter(value = " ")
                ),
                com.klogviewer.domain.model.PatternSegment.Token(
                    com.klogviewer.domain.model.PatternToken(role = com.klogviewer.domain.model.PatternTokenRole.LOGGER)
                ),
                com.klogviewer.domain.model.PatternSegment.Delimiter(
                    com.klogviewer.domain.model.PatternDelimiter(value = " - ")
                ),
                com.klogviewer.domain.model.PatternSegment.Token(
                    com.klogviewer.domain.model.PatternToken(
                        role = com.klogviewer.domain.model.PatternTokenRole.MESSAGE
                    )
                )
            )
        )
    }

    @Test
    fun `given saved directory mapping when structured json log opened then json parser remains authoritative`() {
        val path = "/var/log/events.json"
        val sampleLines = listOf(
            """{"timestamp":"2026-08-25T10:00:00Z","level":"INFO","message":"Structured event"}"""
        )
        val draft = com.klogviewer.domain.model.PatternDraft(name = "Custom Text Pattern")
        val directoryKey = "local:/var/log"
        val mapping = com.klogviewer.domain.model.DirectoryPatternMapping(
            directoryKey = directoryKey,
            patternDraft = draft
        )
        val testProbe = HeuristicProbe(ParserRegistry())
        val customState = MutableStateFlow(
            KLogViewerState(
                directoryPatternMappings = mapOf(directoryKey to mapping)
            )
        )

        every { localFileSystem.exists(path) } returns true
        every { localFileSystem.isDirectory(path) } returns false
        every { localFileSystem.readLines(path, any()) } returns sampleLines

        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = testProbe,
            logSourceFactory = logSourceFactory,
            state = customState
        )

        val result = loader.performHeuristicDetection(paths = listOf(path), overrideParserName = null).single()

        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("JSON")
    }

    @Test
    fun `given directory path when opened without saved mapping then detects pattern from directory sample files`() {
        val dirPath = "/var/log"
        val sampleFilePath = "/var/log/app.log"
        val sampleLines = listOf("2026-08-25 10:00:00.123 [main] INFO MyService - Hello world")

        every { localFileSystem.exists(dirPath) } returns true
        every { localFileSystem.isDirectory(dirPath) } returns true
        every { localFileSystem.listFiles(dirPath, any()) } returns listOf(sampleFilePath)
        every { localFileSystem.exists(sampleFilePath) } returns true
        every { localFileSystem.isDirectory(sampleFilePath) } returns false
        every { localFileSystem.readLines(sampleFilePath, any()) } returns sampleLines

        val testProbe = HeuristicProbe(ParserRegistry())
        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = testProbe,
            logSourceFactory = logSourceFactory,
            state = MutableStateFlow(KLogViewerState())
        )

        val result = loader.performHeuristicDetection(paths = listOf(dirPath), overrideParserName = null).single()

        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("Standard")
    }

    @Test
    fun `given saved directory mapping when directory path opened then saved mapping is used directly`() {
        val dirPath = "/var/log"
        val sampleFilePath = "/var/log/app.log"
        val sampleLines = listOf("2026-08-25 10:00:00.123 [main] INFO MyService - Hello world")
        val draft = createSampleDraft()
        val directoryKey = "local:/var/log"
        val mapping = com.klogviewer.domain.model.DirectoryPatternMapping(
            directoryKey = directoryKey,
            patternDraft = draft
        )
        val testProbe = HeuristicProbe(ParserRegistry())
        val customState = MutableStateFlow(
            KLogViewerState(directoryPatternMappings = mapOf(directoryKey to mapping))
        )

        every { localFileSystem.exists(dirPath) } returns true
        every { localFileSystem.isDirectory(dirPath) } returns true
        every { localFileSystem.listFiles(dirPath, any()) } returns listOf(sampleFilePath)
        every { localFileSystem.exists(sampleFilePath) } returns true
        every { localFileSystem.isDirectory(sampleFilePath) } returns false
        every { localFileSystem.readLines(sampleFilePath, any()) } returns sampleLines

        val loader = WorkspaceLogLoader(
            localFileSystem = localFileSystem,
            remoteFileSystem = remoteFileSystem,
            logSource = logSource,
            heuristicProbe = testProbe,
            logSourceFactory = logSourceFactory,
            state = customState
        )

        val result = loader.performHeuristicDetection(paths = listOf(dirPath), overrideParserName = null).single()

        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("Custom Saved Logback")
    }
}
