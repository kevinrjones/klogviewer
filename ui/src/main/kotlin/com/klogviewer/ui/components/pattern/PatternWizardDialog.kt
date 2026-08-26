package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.PatternWizardState
import com.klogviewer.domain.model.PreviewTableRow
import com.klogviewer.domain.model.SampleLineSpan
import com.klogviewer.domain.model.SourceWizardEntry
import com.klogviewer.domain.model.SourceWizardStatus
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.theme.KLogViewerColors

@Composable
fun PatternWizardDialog(
    state: PatternWizardState,
    isDarkMode: Boolean,
    onIntent: (KLogViewerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    Dialog(
        onDismissRequest = { onIntent(KLogViewerIntent.ClosePatternWizard) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            modifier = modifier
                .width(if (state.isBannerMode) 800.dp else state.windowWidth.dp)
                .height(if (state.isBannerMode) 130.dp else state.windowHeight.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val isCmdOrCtrl = keyEvent.isMetaPressed || keyEvent.isCtrlPressed
                        when {
                            keyEvent.key == Key.Escape -> {
                                onIntent(KLogViewerIntent.ClosePatternWizard)
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Enter -> {
                                onIntent(KLogViewerIntent.ApplyPatternDraft)
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Z && keyEvent.isShiftPressed -> {
                                onIntent(KLogViewerIntent.RedoPatternDraft)
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Z -> {
                                onIntent(KLogViewerIntent.UndoPatternDraft)
                                true
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            if (state.isBannerMode) {
                // High-Confidence Confirmation Banner Variant
                PatternConfirmationBanner(
                    draftName = state.currentDraft.name,
                    confidenceScore = state.confidenceScore,
                    isDarkMode = isDarkMode,
                    onApply = { onIntent(KLogViewerIntent.ApplyPatternDraft) },
                    onReview = { onIntent(KLogViewerIntent.ExpandPatternBannerToFullWizard) },
                    onSkip = { onIntent(KLogViewerIntent.SkipPatternWizard) }
                )
            } else {
                // Full 5-Zone Pattern Wizard UI
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log Pattern Wizard",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onIntent(KLogViewerIntent.UndoPatternDraft) },
                                enabled = state.canUndo
                            ) {
                                Text("Undo")
                            }
                            OutlinedButton(
                                onClick = { onIntent(KLogViewerIntent.RedoPatternDraft) },
                                enabled = state.canRedo
                            ) {
                                Text("Redo")
                            }
                        }
                    }

                    if (state.sources.size > 1) {
                        WizardSourceSelector(
                            sources = state.sources,
                            activeSourceId = state.activeSourceId,
                            onSelectSource = { onIntent(KLogViewerIntent.SelectPatternWizardSource(it)) }
                        )
                    }

                    // Two-Pane Content Area (Left: Controls & Sample Line Inspector, Right: Live Table Grid Preview)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Importer, Token Bar, Sample Line Inspector, Match Summary
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Zone 1: Header & Importer Bar
                            PatternImporterBar(
                                selectedPresetName = state.currentDraft.name,
                                onPresetSelected = {
                                    onIntent(KLogViewerIntent.ImportPatternString(it))
                                },
                                onImportPattern = {
                                    onIntent(KLogViewerIntent.ImportPatternString(it))
                                },
                                isDirectoryPersistenceEnabled = state.currentDraft.isDirectoryPersistenceEnabled,
                                onDirectoryPersistenceToggled = {
                                    onIntent(KLogViewerIntent.SetDirectoryPersistenceEnabled(it))
                                },
                                onManageMappings = {
                                    onIntent(KLogViewerIntent.ShowDirectoryMappingsDialog)
                                }
                            )

                            // Zone 2: Interactive Token Bar
                            PatternTokenBar(
                                segments = state.currentDraft.segments,
                                hoveredSegmentId = state.hoveredSegmentId,
                                focusedTokenId = state.focusedTokenId,
                                isDarkMode = isDarkMode,
                                onTokenClick = { token ->
                                    onIntent(KLogViewerIntent.OpenPatternTokenConfigPopover(token.id))
                                },
                                onSegmentHovered = { segmentId ->
                                    onIntent(KLogViewerIntent.SetHoveredPatternSegment(segmentId))
                                },
                                onRemoveSegment = { segmentId ->
                                    onIntent(KLogViewerIntent.RemovePatternSegment(segmentId))
                                },
                                onReorderSegment = { segmentId, moveLeft ->
                                    onIntent(KLogViewerIntent.ReorderPatternSegment(segmentId, moveLeft))
                                },
                                onDelimiterUpdated = { delimiterId, newValue ->
                                    onIntent(KLogViewerIntent.UpdatePatternDelimiter(delimiterId, newValue))
                                },
                                onAddToken = { segmentIndex, role ->
                                    onIntent(KLogViewerIntent.AddPatternToken(segmentIndex, role))
                                }
                            )

                            // Zone 3: Sample Line Inspector
                            SampleLineInspector(
                                sampleLines = state.sampleLines,
                                selectedLineIndex = state.selectedLineIndex,
                                getSpansForLine = { line ->
                                    val idx = state.sampleLines.indexOf(line)
                                    if (idx in state.previewSpans.indices) state.previewSpans[idx] else emptyList()
                                },
                                parseErrors = state.parseErrors,
                                hoveredSegmentId = state.hoveredSegmentId,
                                isDarkMode = isDarkMode,
                                onLineIndexChanged = { index ->
                                    onIntent(KLogViewerIntent.SelectPatternSampleLine(index))
                                },
                                onSegmentHovered = { segmentId ->
                                    onIntent(KLogViewerIntent.SetHoveredPatternSegment(segmentId))
                                },
                                onResample = {
                                    onIntent(KLogViewerIntent.ResamplePatternLines)
                                }
                            )

                            // Zone 5: Match Health & Summary
                            PatternMatchSummary(
                                matchedCount = state.matchedLineCount,
                                totalCount = state.totalSampleLineCount,
                                confidenceScore = state.confidenceScore,
                                parseErrors = state.parseErrors,
                                isDiagnosticsDrawerOpen = state.isDiagnosticsDrawerOpen,
                                onToggleDiagnosticsDrawer = {
                                    onIntent(KLogViewerIntent.TogglePatternDiagnosticsDrawer)
                                },
                                showMissingTimestampWarning = !state.currentDraft.hasTimestampToken
                            )
                        }

                        // Right Pane: Live Table Grid Preview
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Zone 4: Live Table Grid Preview
                            val previewCols = if (state.previewColumns.isNotEmpty()) {
                                state.previewColumns
                            } else {
                                extractPreviewColumns(state.currentDraft)
                            }
                            val previewRows = state.previewRows

                            PatternTablePreview(
                                columns = previewCols,
                                rows = previewRows,
                                hoveredColumnName = state.hoveredColumnName,
                                isDarkMode = isDarkMode,
                                onColumnHovered = { col -> onIntent(KLogViewerIntent.SetHoveredPatternColumn(col)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Footer / Action Bar
                    Surface(
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onIntent(KLogViewerIntent.ResetPatternToBestGuess) }) {
                                Text("Reset to Best Guess")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = { onIntent(KLogViewerIntent.SkipPatternWizard) }) {
                                    Text("Skip (Plain Text)")
                                }
                                OutlinedButton(onClick = { onIntent(KLogViewerIntent.ClosePatternWizard) }) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { onIntent(KLogViewerIntent.ApplyPatternDraft) },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) {
                                            KLogViewerColors.DarkPrimary
                                        } else {
                                            KLogViewerColors.LightPrimary
                                        },
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Apply & Load (⌘↵)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Active Popovers
    if (state.activePopoverTokenId != null) {
        val targetToken = state.currentDraft.segments
            .filterIsInstance<PatternSegment.Token>()
            .map { it.token }
            .find { it.id == state.activePopoverTokenId }

        if (targetToken != null) {
            PatternTokenConfigPopover(
                token = targetToken,
                onTokenUpdated = { token -> onIntent(KLogViewerIntent.UpdatePatternToken(token)) },
                onTokenDeleted = { tokenId -> onIntent(KLogViewerIntent.RemovePatternSegment(tokenId)) },
                onDismissRequest = { onIntent(KLogViewerIntent.ClosePatternTokenConfigPopover) }
            )
        }
    }

    val selectionRange = state.activeSelectionPopupRange
    if (selectionRange != null) {
        val selectedText = state.sampleLines.getOrNull(state.selectedLineIndex)?.let { line ->
            line.substring(selectionRange.first.coerceIn(0, line.length), (selectionRange.last + 1).coerceIn(0, line.length))
        } ?: ""

        SampleLineSelectionPopup(
            selectedText = selectedText,
            selectedRange = selectionRange,
            onExtractAsToken = { range, role, customName ->
                onIntent(KLogViewerIntent.ExtractSpanAsPatternToken(range, role, customName))
            },
            onDismissRequest = { onIntent(KLogViewerIntent.ClosePatternSelectionPopup) }
        )
    }
}

@Composable
fun PatternConfirmationBanner(
    draftName: String,
    confidenceScore: Float,
    isDarkMode: Boolean = true,
    onApply: () -> Unit,
    onReview: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Detected pattern layout: $draftName",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "High confidence match (${(confidenceScore * 100).toInt()}%). Click Apply to open or Review to customize.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSkip) {
                Text("Skip", maxLines = 1)
            }
            OutlinedButton(onClick = onReview) {
                Text("Review", maxLines = 1)
            }
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) {
                        KLogViewerColors.DarkPrimary
                    } else {
                        KLogViewerColors.LightPrimary
                    },
                    contentColor = Color.White
                )
            ) {
                Text("Apply", maxLines = 1)
            }
        }
    }
}

@Composable
private fun WizardSourceSelector(
    sources: List<SourceWizardEntry>,
    activeSourceId: String?,
    onSelectSource: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sources:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        sources.forEach { source ->
            val isActive = source.sourceId == activeSourceId
            val statusIcon = if (source.status == SourceWizardStatus.SAVED) "✓" else "⚠"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isActive) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .clickable { onSelectSource(source.sourceId) }
                    .testTag("wizard_source_${source.displayName}")
            ) {
                Text(
                    text = "$statusIcon ${source.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun extractPreviewColumns(draft: PatternDraft): List<String> {
    return draft.segments
        .filterIsInstance<PatternSegment.Token>()
        .map { it.token.effectiveName }
}

@Preview
@Composable
fun PatternWizardDialogPreview() {
    val state = PatternWizardState(
        isVisible = true,
        sampleLines = listOf("2026-08-25 10:00:00 [main] INFO App - Booted")
    )

    MaterialTheme {
        Surface {
            PatternWizardDialog(
                state = state,
                isDarkMode = true,
                onIntent = {}
            )
        }
    }
}
