package com.klogviewer.integration

import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.JsonPreferencesRepository
import com.klogviewer.core.repository.InMemorySecureCredentialStore
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.DirectoryIdentityNormalizer
import com.klogviewer.domain.model.DirectoryPatternMapping
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.TabPreference
import com.klogviewer.domain.model.UserPreferences
import com.klogviewer.domain.model.WindowPreference
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class SessionRestorationTest {
    @TempDir
    lateinit var tempDir: File

    private var viewModel: KLogViewerViewModel? = null

    @AfterEach
    fun tearDown() = runBlocking {
        viewModel?.clear()
        kotlinx.coroutines.delay(100.milliseconds)
    }

    @Test
    fun `should not show missing file dialog on session restoration but mark file as missing`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val missingFilePath = File(tempDir, "non-existent.log").absolutePath
        
        // 1. Prepare preferences with a missing file
        val prefsRepo = JsonPreferencesRepository(prefsDir, InMemorySecureCredentialStore())
        val initialPrefs = UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "non-existent.log",
                    windows = listOf(
                        WindowPreference(
                            id = "window1",
                            filePath = missingFilePath,
                            sourceIds = listOf(missingFilePath)
                        )
                    ),
                    activeWindowId = "window1"
                )
            ),
            activeTabId = "tab1"
        )
        prefsRepo.save(initialPrefs)

        // 2. Initialize ViewModel (restores session)
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        // 3. Verify that NO missing file dialog is shown
        expectThat(vm.state.value.pendingDialog).isNull()

        // 4. Verify that the window is marked as missing
        // Wait a bit for the flow to emit the failure
        kotlinx.coroutines.delay(200.milliseconds)
        
        val window = vm.state.value.tabs.flatMap { it.windows }.find { it.id == "window1" }
        expectThat(window).isNotNull().and {
            get { missingSourceIds }.contains(missingFilePath)
            get { error }.isNotNull() // FileLogSource emits an error if file is missing
        }
    }

    @Test
    fun `should restore custom pattern mapping and populate thread and logger fields on restart`(): Unit = runBlocking {
        val prefsDir = File(tempDir, "prefs")
        val logFile = File(tempDir, "application.log")
        logFile.writeText(
            "2026-08-25 10:00:00.123 [main] INFO com.example.MyService - Initialized service successfully\n"
        )

        val directoryKey = DirectoryIdentityNormalizer.normalize(logFile.absolutePath)
        val draft = createSampleLogbackDraft()
        val prefsRepo = JsonPreferencesRepository(prefsDir, InMemorySecureCredentialStore())
        prefsRepo.save(createPrefsWithPattern(logFile.absolutePath, directoryKey, draft))

        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        viewModel = KLogViewerViewModel(source, prefsRepo, heuristicProbe)
        val vm = viewModel!!

        kotlinx.coroutines.delay(500.milliseconds)

        val window = vm.state.value.tabs.flatMap { it.windows }.find { it.id == "window1" }
        expectThat(window).isNotNull().and {
            get { logs }.hasSize(1)
        }
        val entry = window!!.logs.first()
        expectThat(entry.fields["thread"]).isEqualTo("main")
        expectThat(entry.fields["logger"]).isEqualTo("com.example.MyService")
        expectThat(entry.content.value).isEqualTo("Initialized service successfully")
    }

    private fun createSampleLogbackDraft(): PatternDraft {
        return PatternDraft(
            name = "Custom Logback Pattern",
            segments = listOf(
                PatternSegment.Token(
                    PatternToken(
                        role = PatternTokenRole.TIMESTAMP,
                        formatPattern = "yyyy-MM-dd HH:mm:ss.SSS"
                    )
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

    private fun createPrefsWithPattern(filePath: String, directoryKey: String, draft: PatternDraft): UserPreferences {
        return UserPreferences(
            tabs = listOf(
                TabPreference(
                    id = "tab1",
                    title = "application.log",
                    windows = listOf(
                        WindowPreference(
                            id = "window1",
                            filePath = filePath,
                            sourceIds = listOf(filePath),
                            parserName = "Custom Logback Pattern",
                            patternDraft = draft
                        )
                    ),
                    activeWindowId = "window1"
                )
            ),
            activeTabId = "tab1",
            directoryPatternMappings = mapOf(
                directoryKey to DirectoryPatternMapping(
                    directoryKey = directoryKey,
                    patternDraft = draft
                )
            )
        )
    }
}
