package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.JsonLogParser
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.core.parser.TemplateLogParser
import com.klogviewer.domain.model.DirectoryIdentityNormalizer
import com.klogviewer.domain.model.DirectoryPatternMapping
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.SourcePatternRef
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.domain.repository.LogSource
import com.klogviewer.domain.repository.LogSourceFactory
import com.klogviewer.domain.repository.RemoteFileSystem
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TabState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PatternWizardRegressionTest {

    private fun createSampleLogbackDraft(name: String = "Logback Standard"): PatternDraft {
        return PatternDraft(
            name = name,
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss.SSS")
                ),
                PatternSegment.Delimiter(PatternDelimiter(value = " [")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                PatternSegment.Delimiter(PatternDelimiter(value = " ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.LOGGER)),
                PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
            )
        )
    }

    // -------------------------------------------------------------------------
    // 13.10.3: Reopening wizard after apply, cancel, and directory mapping reuse
    // -------------------------------------------------------------------------

    @Test
    fun `given applied pattern when wizard reopened then draft is preloaded from active source pattern`() {
        val draft = createSampleLogbackDraft("Applied Custom Draft")
        val initialWindowState = KLogViewerState(
            tabs = listOf(
                TabState(
                    id = "tab1",
                    title = "app.log",
                    activeWindowId = "win1",
                    windows = listOf(
                        LogWindow(
                            id = "win1",
                            filePath = "/var/log/app.log",
                            sourceIds = listOf("/var/log/app.log"),
                            sourcePatterns = mapOf(
                                "/var/log/app.log" to SourcePatternRef(
                                    parserName = "Applied Custom Draft",
                                    directoryMappingKey = "local:/var/log"
                                )
                            ),
                            patternDraft = draft
                        )
                    )
                )
            ),
            activeTabId = "tab1",
            directoryPatternMappings = mapOf(
                "local:/var/log" to DirectoryPatternMapping("local:/var/log", draft)
            )
        )
        val state = MutableStateFlow(initialWindowState)
        val handler = PatternWizardIntentHandler(state = state, onSampleSourceLines = {
            listOf("2026-08-25 10:00:00.123 [main] INFO com.example.App - Applied test line")
        })

        handler.handle(KLogViewerIntent.OpenPatternWizard(targetWindowId = "win1"))

        val wizard = state.value.patternWizardState
        expectThat(wizard.isVisible).isTrue()
        expectThat(wizard.currentDraft.name).isEqualTo("Applied Custom Draft")
    }

    @Test
    fun `given cancelled wizard when reopened then wizard reinitializes with active source draft`() {
        val draft = createSampleLogbackDraft("Base Pattern")
        val initialWindowState = KLogViewerState(
            tabs = listOf(
                TabState(
                    id = "tab1",
                    title = "app.log",
                    activeWindowId = "win1",
                    windows = listOf(
                        LogWindow(
                            id = "win1",
                            filePath = "/var/log/app.log",
                            sourceIds = listOf("/var/log/app.log"),
                            patternDraft = draft
                        )
                    )
                )
            ),
            activeTabId = "tab1"
        )
        val state = MutableStateFlow(initialWindowState)
        val handler = PatternWizardIntentHandler(state = state, onSampleSourceLines = {
            listOf("2026-08-25 10:00:00.123 [main] INFO com.example.App - Base line")
        })

        // Open and modify
        handler.handle(KLogViewerIntent.OpenPatternWizard(targetWindowId = "win1"))
        handler.handle(KLogViewerIntent.AddPatternToken(0, PatternTokenRole.CUSTOM_PROPERTY))
        expectThat(state.value.patternWizardState.isVisible).isTrue()

        // Cancel
        handler.handle(KLogViewerIntent.ClosePatternWizard)
        expectThat(state.value.patternWizardState.isVisible).isFalse()

        // Reopen
        handler.handle(KLogViewerIntent.OpenPatternWizard(targetWindowId = "win1"))
        val wizardState = state.value.patternWizardState
        expectThat(wizardState.isVisible).isTrue()
        expectThat(wizardState.currentDraft.name).isEqualTo("Base Pattern")
    }

    // -------------------------------------------------------------------------
    // 13.10.4: SFTP / S3 / Local directory identity normalization & mapping reuse
    // -------------------------------------------------------------------------

    @Test
    fun `given saved SFTP directory mapping when SFTP log opened then saved mapping is resolved`() {
        val sftpPath = "sftp://admin@192.168.1.50:22/var/log/syslog.log"
        val directoryKey = "sftp:admin@192.168.1.50:22/var/log"
        val draft = createSampleLogbackDraft("SFTP Logback Pattern")
        val mapping = DirectoryPatternMapping(directoryKey = directoryKey, patternDraft = draft, sourceType = "SFTP")

        val state = MutableStateFlow(
            KLogViewerState(directoryPatternMappings = mapOf(directoryKey to mapping))
        )
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)
        val probe = HeuristicProbe(ParserRegistry())
        val resolver = SourcePatternResolver(localFileSystem, probe, state)

        val sampleLines = listOf("2026-08-25 10:00:00.123 [main] INFO RemoteService - SFTP message")
        val result = resolver.resolveParserForSource(
            path = sftpPath,
            sampleLines = sampleLines,
            overrideParserName = null,
            getParserResultByName = { name, _ ->
                ProbeResult(parser = JsonLogParser(), parserName = name, columns = emptyList())
            }
        )

        expectThat(result.parserName).isEqualTo("SFTP Logback Pattern")
        expectThat(result.parser is TemplateLogParser).isTrue()
    }

    @Test
    fun `given saved S3 directory mapping when S3 log opened then saved mapping is resolved`() {
        val s3Path = "s3://my-prod-bucket/logs/2026/08/app.log"
        val directoryKey = "s3:my-prod-bucket/logs/2026/08"
        val draft = createSampleLogbackDraft("S3 Logback Pattern")
        val mapping = DirectoryPatternMapping(directoryKey = directoryKey, patternDraft = draft, sourceType = "S3")

        val state = MutableStateFlow(
            KLogViewerState(directoryPatternMappings = mapOf(directoryKey to mapping))
        )
        val localFileSystem = mockk<LocalFileSystem>(relaxed = true)
        val probe = HeuristicProbe(ParserRegistry())
        val resolver = SourcePatternResolver(localFileSystem, probe, state)

        val sampleLines = listOf("2026-08-25 10:00:00.123 [main] INFO S3Service - S3 message")
        val result = resolver.resolveParserForSource(
            path = s3Path,
            sampleLines = sampleLines,
            overrideParserName = null,
            getParserResultByName = { name, _ ->
                ProbeResult(parser = JsonLogParser(), parserName = name, columns = emptyList())
            }
        )

        expectThat(result.parserName).isEqualTo("S3 Logback Pattern")
    }

    // -------------------------------------------------------------------------
    // 13.10.5: Single-source window behavior
    // -------------------------------------------------------------------------

    @Test
    fun `given single source log window when created then sourceIds and sourcePatterns are populated identically`() {
        val filePath = "/var/log/single.log"
        val draft = createSampleLogbackDraft("Single Draft")
        val window = LogWindow(
            id = "w1",
            filePath = filePath,
            sourceIds = listOf(filePath),
            parserName = "Single Draft",
            patternDraft = draft,
            sourcePatterns = mapOf(filePath to SourcePatternRef(parserName = "Single Draft"))
        )

        expectThat(window.sourceIds).isEqualTo(listOf(filePath))
        expectThat(window.sourcePatterns.size).isEqualTo(1)
        expectThat(window.sourcePatterns[filePath]?.parserName).isEqualTo("Single Draft")
        expectThat(window.parserName).isEqualTo("Single Draft")
    }

    // -------------------------------------------------------------------------
    // 13.11.2: UI and ViewModel test coverage
    // -------------------------------------------------------------------------

    @Test
    fun `given first open text log when heuristic detection matches then prompt state initialized`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val localFs = mockk<LocalFileSystem>()
        val remoteFs = mockk<RemoteFileSystem>()
        val logSource = mockk<LogSource>(relaxed = true)
        val logSourceFactory = mockk<LogSourceFactory>(relaxed = true)
        val state = MutableStateFlow(KLogViewerState())

        val path = "/var/log/unrecognized.log"
        val lines = listOf("2026-08-25 10:00:00.123 [main] INFO UnrecognizedService - Some text payload")

        every { localFs.exists(path) } returns true
        every { localFs.isDirectory(path) } returns false
        every { localFs.readLines(path, any()) } returns lines

        val probe = HeuristicProbe(ParserRegistry())
        val loader = WorkspaceLogLoader(
            localFileSystem = localFs,
            remoteFileSystem = remoteFs,
            logSource = logSource,
            heuristicProbe = probe,
            logSourceFactory = logSourceFactory,
            state = state
        )

        val results = loader.performHeuristicDetection(paths = listOf(path), overrideParserName = null)
        expectThat(results.size).isEqualTo(1)
        val result = results.first()
        expectThat(result).isNotNull()
        expectThat(result?.parserName).isEqualTo("Standard")
    }
}
