package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.PatternWizardState
import com.klogviewer.domain.model.PreviewTableRow
import com.klogviewer.domain.model.SampleLineSpan

@Composable
fun PatternWizardDialog(
    state: PatternWizardState,
    isDarkMode: Boolean,
    onPresetSelected: (String) -> Unit,
    onImportPattern: (String) -> Unit,
    onDirectoryPersistenceToggled: (Boolean) -> Unit,
    onTokenClick: (PatternToken) -> Unit,
    onSegmentHovered: (String?) -> Unit,
    onRemoveSegment: (String) -> Unit,
    onReorderSegment: (String, Boolean) -> Unit,
    onDelimiterUpdated: (String, String) -> Unit,
    onAddToken: (Int, PatternTokenRole) -> Unit,
    onLineIndexChanged: (Int) -> Unit,
    onColumnHovered: (String?) -> Unit,
    onExtractSpanAsToken: (IntRange, PatternTokenRole, String?) -> Unit,
    onTokenUpdated: (PatternToken) -> Unit,
    onTokenDeleted: (String) -> Unit,
    onToggleDiagnosticsDrawer: () -> Unit,
    onResetToBestGuess: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCancel: () -> Unit,
    onSkip: () -> Unit,
    onApply: () -> Unit,
    onExpandBannerToFullWizard: () -> Unit,
    onClosePopover: () -> Unit,
    onCloseSelectionPopup: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
            modifier = modifier
                .width(state.windowWidth.dp)
                .height(if (state.isBannerMode) 130.dp else state.windowHeight.dp)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        val isCmdOrCtrl = keyEvent.isMetaPressed || keyEvent.isCtrlPressed
                        when {
                            keyEvent.key == Key.Escape -> {
                                onCancel()
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Enter -> {
                                onApply()
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Z && keyEvent.isShiftPressed -> {
                                onRedo()
                                true
                            }
                            isCmdOrCtrl && keyEvent.key == Key.Z -> {
                                onUndo()
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
                    onApply = onApply,
                    onReview = onExpandBannerToFullWizard,
                    onSkip = onSkip
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
                            OutlinedButton(onClick = onUndo, enabled = state.canUndo) {
                                Text("Undo")
                            }
                            OutlinedButton(onClick = onRedo, enabled = state.canRedo) {
                                Text("Redo")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Zone 1: Header & Importer Bar
                        PatternImporterBar(
                            selectedPresetName = state.currentDraft.name,
                            onPresetSelected = onPresetSelected,
                            onImportPattern = onImportPattern,
                            isDirectoryPersistenceEnabled = state.currentDraft.isDirectoryPersistenceEnabled,
                            onDirectoryPersistenceToggled = onDirectoryPersistenceToggled
                        )

                        // Zone 2: Interactive Token Bar
                        PatternTokenBar(
                            segments = state.currentDraft.segments,
                            hoveredSegmentId = state.hoveredSegmentId,
                            focusedTokenId = state.focusedTokenId,
                            isDarkMode = isDarkMode,
                            onTokenClick = onTokenClick,
                            onSegmentHovered = onSegmentHovered,
                            onRemoveSegment = onRemoveSegment,
                            onReorderSegment = onReorderSegment,
                            onDelimiterUpdated = onDelimiterUpdated,
                            onAddToken = onAddToken
                        )

                        // Zone 3: Sample Line Inspector
                        SampleLineInspector(
                            sampleLines = state.sampleLines,
                            selectedLineIndex = state.selectedLineIndex,
                            getSpansForLine = { line -> mapDraftToSampleSpans(state.currentDraft, line) },
                            parseErrors = state.parseErrors,
                            hoveredSegmentId = state.hoveredSegmentId,
                            isDarkMode = isDarkMode,
                            onLineIndexChanged = onLineIndexChanged,
                            onSegmentHovered = onSegmentHovered
                        )

                        // Zone 4: Live Table Grid Preview
                        val previewCols = extractPreviewColumns(state.currentDraft)
                        val previewRows = generatePreviewRows(state.currentDraft, state.sampleLines)

                        PatternTablePreview(
                            columns = previewCols,
                            rows = previewRows,
                            hoveredColumnName = state.hoveredColumnName,
                            isDarkMode = isDarkMode,
                            onColumnHovered = onColumnHovered
                        )

                        // Zone 5: Match Health & Summary
                        PatternMatchSummary(
                            matchedCount = state.matchedLineCount,
                            totalCount = state.totalSampleLineCount,
                            confidenceScore = state.confidenceScore,
                            parseErrors = state.parseErrors,
                            isDiagnosticsDrawerOpen = state.isDiagnosticsDrawerOpen,
                            onToggleDiagnosticsDrawer = onToggleDiagnosticsDrawer
                        )
                    }

                    // Footer / Action Bar
                    Surface(
                        tonalElevation = 2.dp,
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onResetToBestGuess) {
                                Text("Reset to Best Guess")
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(onClick = onSkip) {
                                    Text("Skip (Plain Text)")
                                }
                                OutlinedButton(onClick = onCancel) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = onApply,
                                    shape = RoundedCornerShape(8.dp)
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
                onTokenUpdated = onTokenUpdated,
                onTokenDeleted = onTokenDeleted,
                onDismissRequest = onClosePopover
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
            onExtractAsToken = onExtractSpanAsToken,
            onDismissRequest = onCloseSelectionPopup
        )
    }
}

@Composable
fun PatternConfirmationBanner(
    draftName: String,
    confidenceScore: Float,
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
            Button(onClick = onApply) {
                Text("Apply", maxLines = 1)
            }
        }
    }
}

private fun extractPreviewColumns(draft: PatternDraft): List<String> {
    return draft.segments
        .filterIsInstance<PatternSegment.Token>()
        .map { it.token.effectiveName }
}

private fun generatePreviewRows(draft: PatternDraft, sampleLines: List<String>): List<PreviewTableRow> {
    val cols = extractPreviewColumns(draft)
    return sampleLines.mapIndexed { idx, line ->
        val map = mutableMapOf<String, String>()
        val tokens = draft.segments.filterIsInstance<PatternSegment.Token>().map { it.token }
        val parts = line.split(Regex("\\s+"))
        tokens.forEachIndexed { tokenIdx, token ->
            val valStr = parts.getOrNull(tokenIdx) ?: ""
            map[token.effectiveName] = valStr
        }
        PreviewTableRow(idx, map)
    }
}

private fun mapDraftToSampleSpans(draft: PatternDraft, sampleLine: String): List<SampleLineSpan> {
    if (sampleLine.isEmpty()) return emptyList()
    val spans = mutableListOf<SampleLineSpan>()
    var currentOffset = 0

    draft.segments.forEach { segment ->
        when (segment) {
            is PatternSegment.Delimiter -> {
                val valStr = segment.delimiter.value
                if (valStr.isNotEmpty()) {
                    val foundIdx = sampleLine.indexOf(valStr, currentOffset)
                    if (foundIdx != -1) {
                        currentOffset = foundIdx + valStr.length
                    }
                }
            }
            is PatternSegment.Token -> {
                val nextDelimiter = draft.segments
                    .dropWhile { it != segment }
                    .drop(1)
                    .filterIsInstance<PatternSegment.Delimiter>()
                    .firstOrNull { it.delimiter.value.isNotEmpty() }
                    ?.delimiter?.value

                val endIdx = if (nextDelimiter != null) {
                    val idx = sampleLine.indexOf(nextDelimiter, currentOffset)
                    if (idx != -1 && idx >= currentOffset) idx else sampleLine.length
                } else {
                    sampleLine.length
                }

                if (currentOffset < endIdx && currentOffset < sampleLine.length) {
                    val validEnd = endIdx.coerceAtMost(sampleLine.length)
                    spans.add(
                        SampleLineSpan(
                            range = currentOffset until validEnd,
                            segmentId = segment.id,
                            role = segment.token.role
                        )
                    )
                    currentOffset = validEnd
                }
            }
        }
    }
    return spans
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
                onPresetSelected = {},
                onImportPattern = {},
                onDirectoryPersistenceToggled = {},
                onTokenClick = {},
                onSegmentHovered = {},
                onRemoveSegment = {},
                onReorderSegment = { _, _ -> },
                onDelimiterUpdated = { _, _ -> },
                onAddToken = { _, _ -> },
                onLineIndexChanged = {},
                onColumnHovered = {},
                onExtractSpanAsToken = { _, _, _ -> },
                onTokenUpdated = {},
                onTokenDeleted = {},
                onToggleDiagnosticsDrawer = {},
                onResetToBestGuess = {},
                onUndo = {},
                onRedo = {},
                onCancel = {},
                onSkip = {},
                onApply = {},
                onExpandBannerToFullWizard = {},
                onClosePopover = {},
                onCloseSelectionPopup = {}
            )
        }
    }
}
