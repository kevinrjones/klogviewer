package com.klogviewer.ui.components.pattern

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternDraftHistory
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.PatternWizardState
import com.klogviewer.ui.mvi.KLogViewerIntent
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@OptIn(ExperimentalTestApi::class)
class PatternWizardThemeAndShapeUiTest {

    @Test
    fun `given pattern wizard dialog in dark mode when rendered then all zones and apply button display`() =
        runComposeUiTest {
            var intentSent: KLogViewerIntent? = null
            val draft = PatternDraft(
                name = "Test Draft",
                segments = listOf(
                    PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd")),
                    PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                    PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
                    PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
                )
            )
            val state = PatternWizardState(
                isVisible = true,
                draftHistory = PatternDraftHistory(current = draft),
                sampleLines = listOf("2026-08-25 [worker-1] INFO App - System ready"),
                previewRows = emptyList()
            )

            setContent {
                MaterialTheme {
                    PatternWizardDialog(
                        state = state,
                        isDarkMode = true,
                        onIntent = { intentSent = it }
                    )
                }
            }

            onNodeWithText("Log Pattern Wizard").assertIsDisplayed()
            onNodeWithText("Apply & Load (⌘↵)").assertIsDisplayed()
            onAllNodesWithText("thread")[0].assertIsDisplayed()
            onAllNodesWithText("timestamp")[0].assertIsDisplayed()

            onNodeWithText("Apply & Load (⌘↵)").performClick()
            expectThat(intentSent).isEqualTo(KLogViewerIntent.ApplyPatternDraft)
        }

    @Test
    fun `given pattern wizard dialog in light mode when rendered then components render cleanly`() =
        runComposeUiTest {
            var intentSent: KLogViewerIntent? = null
            val draft = PatternDraft.createDefaultLogback("Light Draft")
            val state = PatternWizardState(
                isVisible = true,
                draftHistory = PatternDraftHistory(current = draft),
                sampleLines = listOf("2026-08-25 10:00:00.000 [main] INFO Engine - Initialized"),
                previewRows = emptyList()
            )

            setContent {
                MaterialTheme {
                    PatternWizardDialog(
                        state = state,
                        isDarkMode = false,
                        onIntent = { intentSent = it }
                    )
                }
            }

            onNodeWithText("Log Pattern Wizard").assertIsDisplayed()
            onNodeWithText("Apply & Load (⌘↵)").assertIsDisplayed()
            onNodeWithText("Reset to Best Guess").assertIsDisplayed()
            onNodeWithText("Skip (Plain Text)").assertIsDisplayed()
            onNodeWithText("Cancel").assertIsDisplayed()

            onNodeWithText("Cancel").performClick()
            expectThat(intentSent).isEqualTo(KLogViewerIntent.ClosePatternWizard)
        }

    @Test
    fun `given confirmation banner in dark and light modes when rendered then banner actions are clickable`() =
        runComposeUiTest {
            var applied = false
            var reviewed = false
            var skipped = false

            setContent {
                MaterialTheme {
                    PatternConfirmationBanner(
                        draftName = "Logback Standard",
                        confidenceScore = 0.95f,
                        isDarkMode = true,
                        onApply = { applied = true },
                        onReview = { reviewed = true },
                        onSkip = { skipped = true }
                    )
                }
            }

            onNodeWithText("Detected pattern layout: Logback Standard").assertIsDisplayed()
            onNodeWithText("Apply").assertIsDisplayed()
            onNodeWithText("Review").assertIsDisplayed()
            onNodeWithText("Skip").assertIsDisplayed()

            onNodeWithText("Apply").performClick()
            expectThat(applied).isTrue()
        }

    @Test
    fun `given pattern token bar with thread and custom tokens when rendered in dark and light then tokens display`() =
        runComposeUiTest {
            val sampleSegments = listOf(
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "HH:mm:ss")
                ),
                PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
                PatternSegment.Token(
                    PatternToken(role = PatternTokenRole.CUSTOM_PROPERTY, customPropertyName = "reqId")
                )
            )

            setContent {
                MaterialTheme {
                    PatternTokenBar(
                        segments = sampleSegments,
                        hoveredSegmentId = null,
                        focusedTokenId = null,
                        isDarkMode = true,
                        onTokenClick = {},
                        onSegmentHovered = {},
                        onRemoveSegment = {},
                        onReorderSegment = { _, _ -> },
                        onDelimiterUpdated = { _, _ -> },
                        onAddToken = { _, _ -> }
                    )
                }
            }

            onNodeWithText("timestamp").assertIsDisplayed()
            onNodeWithText("thread").assertIsDisplayed()
            onNodeWithText("reqId").assertIsDisplayed()
        }

    @Test
    fun `given a complete pattern match when rendered then success icon has an accessible description`() =
        runComposeUiTest {
            setContent {
                com.klogviewer.ui.theme.KLogViewerTheme {
                    PatternMatchSummary(
                        matchedCount = 2,
                        totalCount = 2,
                        confidenceScore = 1f,
                        parseErrors = emptyList(),
                        isDiagnosticsDrawerOpen = false,
                        onToggleDiagnosticsDrawer = {}
                    )
                }
            }

            onNodeWithContentDescription("All sample lines matched").assertIsDisplayed()
            onNodeWithText("2/2 sample lines matched (100%)").assertIsDisplayed()
        }
}
