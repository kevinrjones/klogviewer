package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.DefaultPatternPreviewService
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.PatternDraftCompiler
import com.klogviewer.core.parser.PatternPreviewService
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.core.parser.TemplateLogParser
import com.klogviewer.domain.model.DirectoryIdentityNormalizer
import com.klogviewer.domain.model.DirectoryPatternMapping
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternPreviewResult
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class DirectoryMappingCoordinator(
    private val state: MutableStateFlow<KLogViewerState>,
    private val heuristicProbe: HeuristicProbe,
    private val workspaceLogLoader: WorkspaceLogLoader,
    private val patternPreviewService: PatternPreviewService = DefaultPatternPreviewService(),
    private val onSavePreferences: () -> Unit = {}
) {
    fun applyPatternDraft(
        windowId: String,
        draft: PatternDraft,
        paths: List<String>,
        isDirectory: Boolean,
        onReload: (String, List<String>, String) -> Unit
    ) {
        val compiled = PatternDraftCompiler().compile(draft)
        heuristicProbe.registry.register(compiled.template)

        if (draft.isDirectoryPersistenceEnabled) {
            val path = paths.firstOrNull() ?: ""
            if (path.isNotEmpty()) {
                val directoryKey = DirectoryIdentityNormalizer.normalize(path, isDirectory = isDirectory)
                val sourceType = DirectoryIdentityNormalizer.extractSourceType(directoryKey)
                val mapping = DirectoryPatternMapping(
                    directoryKey = directoryKey,
                    patternDraft = draft,
                    sourceType = sourceType,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis()
                )
                state.update { currentState ->
                    currentState.copy(
                        directoryPatternMappings = currentState.directoryPatternMappings + (directoryKey to mapping)
                    )
                }
                onSavePreferences()
            }
        }

        state.update { currentState ->
            currentState.updateWindow(windowId) { w ->
                val probeResult = ProbeResult(
                    TemplateLogParser(compiled.template),
                    compiled.template.name,
                    compiled.template.columns
                )
                w.copy(
                    parserName = compiled.template.name,
                    patternDraft = draft,
                    columns = LogLoadingCoordinator.mergeColumnsWithDiscoveredStatic(w.columns, listOf(probeResult))
                )
            }
        }

        if (paths.isNotEmpty()) {
            onReload(windowId, paths, compiled.template.name)
        }
    }

    fun handleSavedDirectoryMapping(
        windowId: String,
        path: String,
        directoryKey: String,
        savedMapping: DirectoryPatternMapping,
        results: List<ProbeResult?>,
        overrideParserName: String?
    ): Boolean {
        val sampleLines = workspaceLogLoader.readSampleLines(path, limit = 20)
        val previewResult = patternPreviewService.computePreview(savedMapping.patternDraft, sampleLines)
        val isMatchValid = previewResult.confidenceScore >= MATCH_CONFIDENCE_THRESHOLD

        return if (isMatchValid) {
            applyValidSavedMapping(windowId, directoryKey, savedMapping, results)
            true
        } else {
            recoverMismatchedSavedMapping(
                windowId = windowId,
                sampleLines = sampleLines,
                savedMapping = savedMapping,
                previewResult = previewResult,
                results = results,
                overrideParserName = overrideParserName
            )
            true
        }
    }

    private fun applyValidSavedMapping(
        windowId: String,
        directoryKey: String,
        savedMapping: DirectoryPatternMapping,
        results: List<ProbeResult?>
    ) {
        val updatedMapping = savedMapping.copy(lastUsedAt = System.currentTimeMillis())
        state.update { currentState ->
            val nextMappings = currentState.directoryPatternMappings + (directoryKey to updatedMapping)
            val updatedState = currentState.copy(directoryPatternMappings = nextMappings)
            updatedState.updateWindow(windowId) { window ->
                window.copy(
                    columns = LogLoadingCoordinator.mergeColumnsWithDiscoveredStatic(window.columns, results),
                    parserName = results.firstOrNull()?.parserName ?: "Auto",
                    patternDraft = savedMapping.patternDraft
                )
            }
        }
        onSavePreferences()
    }

    private fun recoverMismatchedSavedMapping(
        windowId: String,
        sampleLines: List<String>,
        savedMapping: DirectoryPatternMapping,
        previewResult: PatternPreviewResult,
        results: List<ProbeResult?>,
        overrideParserName: String?
    ) {
        val compiled = PatternDraftCompiler().compile(savedMapping.patternDraft)
        heuristicProbe.registry.register(compiled.template)

        state.update { currentState ->
            val updatedState = currentState.updateWindow(windowId) { window ->
                window.copy(
                    columns = LogLoadingCoordinator.mergeColumnsWithDiscoveredStatic(window.columns, results),
                    parserName = overrideParserName ?: results.firstOrNull()?.parserName ?: "Auto"
                )
            }
            val wizardState = updatedState.patternWizardState.copy(
                isVisible = true,
                isBannerMode = false,
                targetWindowId = windowId,
                sampleLines = sampleLines,
                selectedLineIndex = 0,
                draftHistory = com.klogviewer.domain.model.PatternDraftHistory(current = savedMapping.patternDraft),
                initialBestGuess = savedMapping.patternDraft,
                confidenceScore = previewResult.confidenceScore,
                matchedLineCount = previewResult.matchedLineCount,
                totalSampleLineCount = previewResult.totalSampleLineCount,
                parseErrors = previewResult.parseErrors,
                previewSpans = previewResult.spansPerLine,
                previewRows = previewResult.previewRows,
                previewColumns = previewResult.columns,
                isDiagnosticsDrawerOpen = true
            )
            updatedState.copy(
                pendingDialog = KLogViewerState.DialogType.PATTERN_WIZARD,
                patternWizardState = wizardState
            )
        }
    }

    companion object {
        const val MATCH_CONFIDENCE_THRESHOLD = 0.80f
    }
}
