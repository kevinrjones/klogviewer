package com.klogviewer.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PatternTokenRole(val displayName: String) {
    TIMESTAMP("Timestamp"),
    LEVEL("Level"),
    THREAD("Thread"),
    LOGGER("Logger"),
    MESSAGE("Message"),
    EXCEPTION("Exception"),
    CUSTOM_PROPERTY("Custom Property");

    companion object {
        fun fromDisplayName(name: String): PatternTokenRole {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: CUSTOM_PROPERTY
        }
    }
}

@Serializable
data class PatternToken(
    val id: String = UUID.randomUUID().toString(),
    val role: PatternTokenRole,
    val customPropertyName: String? = null,
    val formatPattern: String = "",
    val isOptional: Boolean = false,
    val regexOverride: String? = null
) {
    val effectiveName: String
        get() = when (role) {
            PatternTokenRole.CUSTOM_PROPERTY -> customPropertyName?.takeIf { it.isNotBlank() } ?: "custom"
            else -> role.displayName.lowercase()
        }
}

@Serializable
data class PatternDelimiter(
    val id: String = UUID.randomUUID().toString(),
    val value: String
)

@Serializable
sealed interface PatternSegment {
    val id: String

    @Serializable
    data class Token(val token: PatternToken) : PatternSegment {
        override val id: String get() = token.id
    }

    @Serializable
    data class Delimiter(val delimiter: PatternDelimiter) : PatternSegment {
        override val id: String get() = delimiter.id
    }
}

@Serializable
data class PatternDraft(
    val id: String = UUID.randomUUID().toString(),
    val segments: List<PatternSegment> = emptyList(),
    val name: String = "Custom Pattern",
    val originalFormatString: String? = null,
    val originalFormatSyntax: String? = null,
    val placeholderAnnotations: Map<String, String> = emptyMap(),
    val isDirectoryPersistenceEnabled: Boolean = true
)

data class PatternDraftHistory(
    val past: List<PatternDraft> = emptyList(),
    val current: PatternDraft = PatternDraft(),
    val future: List<PatternDraft> = emptyList()
) {
    val canUndo: Boolean get() = past.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    fun push(newDraft: PatternDraft): PatternDraftHistory {
        if (newDraft == current) return this
        return PatternDraftHistory(
            past = past + current,
            current = newDraft,
            future = emptyList()
        )
    }

    fun undo(): PatternDraftHistory {
        if (!canUndo) return this
        val previous = past.last()
        return PatternDraftHistory(
            past = past.dropLast(1),
            current = previous,
            future = listOf(current) + future
        )
    }

    fun redo(): PatternDraftHistory {
        if (!canRedo) return this
        val next = future.first()
        return PatternDraftHistory(
            past = past + current,
            current = next,
            future = future.drop(1)
        )
    }

    fun reset(initialDraft: PatternDraft): PatternDraftHistory {
        return PatternDraftHistory(
            past = emptyList(),
            current = initialDraft,
            future = emptyList()
        )
    }
}

data class PatternParseError(
    val lineIndex: Int,
    val lineText: String,
    val errorOffset: Int,
    val message: String
)

data class SampleLineSpan(
    val range: IntRange,
    val segmentId: String,
    val role: PatternTokenRole
)

data class PreviewTableRow(
    val lineIndex: Int,
    val fields: Map<String, String>
)

data class PatternPreviewResult(
    val spansPerLine: List<List<SampleLineSpan>> = emptyList(),
    val previewRows: List<PreviewTableRow> = emptyList(),
    val columns: List<String> = emptyList(),
    val parseErrors: List<PatternParseError> = emptyList(),
    val matchedLineCount: Int = 0,
    val totalSampleLineCount: Int = 0,
    val confidenceScore: Float = 1.0f
)

data class PatternWizardState(
    val isVisible: Boolean = false,
    val isBannerMode: Boolean = false,
    val targetWindowId: String? = null,
    val sampleLines: List<String> = emptyList(),
    val selectedLineIndex: Int = 0,
    val draftHistory: PatternDraftHistory = PatternDraftHistory(),
    val initialBestGuess: PatternDraft? = null,
    val hoveredSegmentId: String? = null,
    val hoveredSampleSpanRange: IntRange? = null,
    val hoveredColumnName: String? = null,
    val focusedTokenId: String? = null,
    val activePopoverTokenId: String? = null,
    val activeSelectionPopupRange: IntRange? = null,
    val confidenceScore: Float = 1.0f,
    val matchedLineCount: Int = 0,
    val totalSampleLineCount: Int = 0,
    val parseErrors: List<PatternParseError> = emptyList(),
    val isDiagnosticsDrawerOpen: Boolean = false,
    val windowWidth: Int = 1840,
    val windowHeight: Int = 780,
    val splitterRatio: Float = 0.45f,
    val previewSpans: List<List<SampleLineSpan>> = emptyList(),
    val previewRows: List<PreviewTableRow> = emptyList(),
    val previewColumns: List<String> = emptyList(),
    val isComputingPreview: Boolean = false
) {
    val currentDraft: PatternDraft get() = draftHistory.current
    val canUndo: Boolean get() = draftHistory.canUndo
    val canRedo: Boolean get() = draftHistory.canRedo
}
