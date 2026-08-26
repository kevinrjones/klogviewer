package com.klogviewer.ui.viewmodel

import com.klogviewer.core.parser.DefaultPatternPreviewService
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.PatternDraftCompiler
import com.klogviewer.core.parser.ProbeResult
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.parser.TemplateLogParser
import com.klogviewer.domain.model.DirectoryIdentityNormalizer
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.SourcePatternRef
import com.klogviewer.domain.repository.LocalFileSystem
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.StateFlow

/**
 * Resolves the parser for each window source independently, layering
 * file overrides over directory mappings over heuristic detection.
 */
class SourcePatternResolver(
    private val localFileSystem: LocalFileSystem,
    private val heuristicProbe: HeuristicProbe,
    private val state: StateFlow<KLogViewerState>
) {
    fun resolveParserForSource(
        path: String,
        sampleLines: List<String>,
        overrideParserName: String?,
        getParserResultByName: (String, List<String>) -> ProbeResult
    ): ProbeResult {
        val isDir = DirectoryIdentityNormalizer.isDirectory(path, localFileSystem)
        val directoryKey = DirectoryIdentityNormalizer.normalize(path, isDirectory = isDir)
        val savedMapping = state.value.directoryPatternMappings[directoryKey]
        val fileOverride = if (isDir) null else {
            state.value.filePatternOverrides[DirectoryIdentityNormalizer.normalizeFile(path)]
        }

        return if (overrideParserName != null && overrideParserName != "Auto") {
            val result = getParserResultByName(overrideParserName, sampleLines)
            if (result.parser is SimpleLogParser && overrideParserName != "Simple" && savedMapping != null) {
                compileDraftResult(savedMapping.patternDraft)
            } else {
                result
            }
        } else {
            compileMappingIfConfident(fileOverride?.patternDraft, sampleLines)
                ?: compileMappingIfConfident(savedMapping?.patternDraft, sampleLines)
                ?: heuristicProbe.detect(sampleLines)
        }
    }

    fun resolveSourcePatternRefs(paths: List<String>, results: List<ProbeResult?>): Map<String, SourcePatternRef> {
        return paths.mapIndexedNotNull { index, path ->
            val result = results.getOrNull(index) ?: return@mapIndexedNotNull null
            path to resolveSourcePatternRef(path, result)
        }.toMap()
    }

    private fun resolveSourcePatternRef(path: String, result: ProbeResult): SourcePatternRef {
        val isDir = DirectoryIdentityNormalizer.isDirectory(path, localFileSystem)
        val fileKey = if (isDir) null else DirectoryIdentityNormalizer.normalizeFile(path)
        val directoryKey = DirectoryIdentityNormalizer.normalize(path, isDirectory = isDir)
        val fileOverride = fileKey?.let { state.value.filePatternOverrides[it] }
        val directoryMapping = directoryKey.let { state.value.directoryPatternMappings[it] }
        return when {
            fileOverride != null && fileOverride.patternDraft.name == result.parserName ->
                SourcePatternRef(parserName = result.parserName, fileOverrideKey = fileKey)
            directoryMapping != null && directoryMapping.patternDraft.name == result.parserName ->
                SourcePatternRef(parserName = result.parserName, directoryMappingKey = directoryKey)
            else -> SourcePatternRef(parserName = result.parserName)
        }
    }

    private fun compileMappingIfConfident(draft: PatternDraft?, sampleLines: List<String>): ProbeResult? {
        if (draft == null) return null
        val previewResult = DefaultPatternPreviewService().computePreview(draft, sampleLines)
        return if (previewResult.confidenceScore >= MATCH_CONFIDENCE_THRESHOLD) {
            compileDraftResult(draft)
        } else {
            null
        }
    }

    private fun compileDraftResult(draft: PatternDraft): ProbeResult {
        val compiled = PatternDraftCompiler().compile(draft)
        heuristicProbe.registry.register(compiled.template)
        return ProbeResult(
            parser = TemplateLogParser(compiled.template),
            parserName = compiled.template.name,
            columns = compiled.template.columns
        )
    }

    companion object {
        const val MATCH_CONFIDENCE_THRESHOLD = 0.80f
    }
}
