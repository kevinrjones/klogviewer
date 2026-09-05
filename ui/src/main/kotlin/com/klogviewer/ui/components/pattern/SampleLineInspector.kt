package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.PatternParseError
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.domain.model.SampleLineSpan

import androidx.compose.ui.text.AnnotatedString

private const val MAX_DISPLAY_SAMPLE_LINES = 5
private val DARK_SELECTED_BG = Color(0xFF2C2C2C)
private val LIGHT_SELECTED_BG = Color(0xFFEBF3FE)
private val DARK_UNSELECTED_BG = Color(0xFF1E1E1E)
private val LIGHT_UNSELECTED_BG = Color(0xFFF5F5F5)
private val PARSE_ERROR_RED = Color(0xFFE74C3C)

@Composable
fun SampleLineInspector(
    sampleLines: List<String>,
    selectedLineIndex: Int,
    getSpansForLine: (String) -> List<SampleLineSpan>,
    parseErrors: List<PatternParseError>,
    hoveredSegmentId: String?,
    isDarkMode: Boolean,
    onLineIndexChanged: (Int) -> Unit,
    onSegmentHovered: (String?) -> Unit,
    onResample: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedIndices by remember { mutableStateOf(setOf<Int>()) }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header / Information Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shownCount = sampleLines.take(MAX_DISPLAY_SAMPLE_LINES).size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sample Line Inspector ($shownCount of ${sampleLines.size} lines shown)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (onResample != null) {
                        OutlinedButton(
                            onClick = onResample,
                            modifier = Modifier.padding(start = 4.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("Resample (Head/Mid/Tail)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (sampleLines.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onLineIndexChanged((selectedLineIndex - 1).coerceAtLeast(0)) },
                            enabled = selectedLineIndex > 0,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Line")
                        }

                        Text(
                            text = "Line ${selectedLineIndex + 1} of ${sampleLines.size}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        IconButton(
                            onClick = {
                                onLineIndexChanged((selectedLineIndex + 1).coerceAtMost(sampleLines.size - 1))
                            },
                            enabled = selectedLineIndex < sampleLines.size - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Line")
                        }
                    }
                }
            }

            // Multi-Line Sample Inspector View (Showing up to 5 sample lines)
            val displayLines = sampleLines.take(MAX_DISPLAY_SAMPLE_LINES)
            if (displayLines.isEmpty()) {
                Text(
                    text = "No sample lines available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayLines.forEachIndexed { index, lineText ->
                        val isSelected = index == selectedLineIndex
                        val parseError = parseErrors.find { it.lineIndex == index }
                        val spans = getSpansForLine(lineText)
                        val isExpanded = index in expandedIndices

                        sampleLineCard(
                            index = index,
                            lineText = lineText,
                            isSelected = isSelected,
                            isExpanded = isExpanded,
                            parseError = parseError,
                            spans = spans,
                            hoveredSegmentId = hoveredSegmentId,
                            isDarkMode = isDarkMode,
                            onLineClicked = { onLineIndexChanged(index) },
                            onToggleExpand = {
                                expandedIndices = if (isExpanded) {
                                    expandedIndices - index
                                } else {
                                    expandedIndices + index
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun sampleLineCard(
    index: Int,
    lineText: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    parseError: PatternParseError?,
    spans: List<SampleLineSpan>,
    hoveredSegmentId: String?,
    isDarkMode: Boolean,
    onLineClicked: () -> Unit,
    onToggleExpand: () -> Unit
) {
    val bgColor = when {
        isSelected && isDarkMode -> DARK_SELECTED_BG
        isSelected -> LIGHT_SELECTED_BG
        isDarkMode -> DARK_UNSELECTED_BG
        else -> LIGHT_UNSELECTED_BG
    }
    val borderColor = when {
        parseError != null -> PARSE_ERROR_RED
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLineClicked)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            sampleLineRow(
                index = index,
                lineText = lineText,
                isSelected = isSelected,
                isExpanded = isExpanded,
                spans = spans,
                hoveredSegmentId = hoveredSegmentId,
                isDarkMode = isDarkMode,
                onToggleExpand = onToggleExpand
            )

            if (parseError != null) {
                Text(
                    text = "⚠️ Parse Error at offset ${parseError.errorOffset}: ${parseError.message}",
                    style = TextStyle(
                        color = PARSE_ERROR_RED,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun sampleLineRow(
    index: Int,
    lineText: String,
    isSelected: Boolean,
    isExpanded: Boolean,
    spans: List<SampleLineSpan>,
    hoveredSegmentId: String?,
    isDarkMode: Boolean,
    onToggleExpand: () -> Unit
) {
    val isLongOrMultiline = lineText.contains('\n') || lineText.length > 120

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(3.dp)
        ) {
            Text(
                text = "#${index + 1}",
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        SelectionContainer(modifier = Modifier.weight(1f)) {
            val annotatedString = buildAnnotatedSampleLine(
                lineText = lineText,
                spans = spans,
                hoveredSegmentId = hoveredSegmentId,
                isDarkMode = isDarkMode
            )
            Text(
                text = annotatedString,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (isLongOrMultiline) {
            TextButton(
                onClick = onToggleExpand,
                modifier = Modifier.padding(0.dp)
            ) {
                Text(
                    text = if (isExpanded) "Collapse" else "Expand",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Backward-compatibility overload
@Composable
fun SampleLineInspector(
    sampleLines: List<String>,
    selectedLineIndex: Int,
    spansForLine: List<SampleLineSpan>,
    parseErrorForLine: PatternParseError?,
    hoveredSegmentId: String?,
    isDarkMode: Boolean,
    onLineIndexChanged: (Int) -> Unit,
    onSegmentHovered: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    SampleLineInspector(
        sampleLines = sampleLines,
        selectedLineIndex = selectedLineIndex,
        getSpansForLine = { line ->
            if (line == sampleLines.getOrNull(selectedLineIndex)) spansForLine else emptyList()
        },
        parseErrors = if (parseErrorForLine != null) listOf(parseErrorForLine) else emptyList(),
        hoveredSegmentId = hoveredSegmentId,
        isDarkMode = isDarkMode,
        onLineIndexChanged = onLineIndexChanged,
        onSegmentHovered = onSegmentHovered,
        modifier = modifier
    )
}

private fun buildAnnotatedSampleLine(
    lineText: String,
    spans: List<SampleLineSpan>,
    hoveredSegmentId: String?,
    isDarkMode: Boolean
): AnnotatedString = buildAnnotatedString {
    if (lineText.isEmpty()) {
        append(" ")
        return@buildAnnotatedString
    }
    if (spans.isEmpty()) {
        append(lineText)
        return@buildAnnotatedString
    }

    var lastIdx = 0
    val validSpans = spans
        .filter {
            !it.range.isEmpty() &&
                it.range.first >= 0 &&
                it.range.last < lineText.length &&
                it.range.first <= it.range.last
        }
        .sortedBy { it.range.first }

    for (span in validSpans) {
        val start = span.range.first
        val end = span.range.last + 1
        if (start > lastIdx && lastIdx < lineText.length) {
            append(lineText.substring(lastIdx, start.coerceAtMost(lineText.length)))
        }
        val spanStart = start.coerceIn(0, lineText.length)
        val spanEnd = end.coerceIn(spanStart, lineText.length)
        if (spanStart < spanEnd) {
            val spanText = lineText.substring(spanStart, spanEnd)
            val bgColor = PatternTheme.roleBackgroundColor(span.role, isDarkMode)
            val textColor = PatternTheme.roleColor(span.role, isDarkMode)
            val isHovered = hoveredSegmentId == span.segmentId

            withStyle(
                style = SpanStyle(
                    background = if (isHovered) bgColor.copy(alpha = 0.5f) else bgColor,
                    color = textColor,
                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Normal
                )
            ) {
                append(spanText)
            }
            lastIdx = spanEnd
        }
    }
    if (lastIdx < lineText.length) {
        append(lineText.substring(lastIdx))
    }
}

@Preview
@Composable
fun SampleLineInspectorPreview() {
    val samples = listOf("2026-08-25 10:15:30.123 [main] INFO com.example.Logger - Application started successfully")
    val sampleSpans = listOf(
        SampleLineSpan(0..22, "1", PatternTokenRole.TIMESTAMP),
        SampleLineSpan(25..28, "2", PatternTokenRole.THREAD),
        SampleLineSpan(31..34, "3", PatternTokenRole.LEVEL),
        SampleLineSpan(36..52, "4", PatternTokenRole.LOGGER),
        SampleLineSpan(56..83, "5", PatternTokenRole.MESSAGE)
    )

    MaterialTheme {
        Surface {
            SampleLineInspector(
                sampleLines = samples,
                selectedLineIndex = 0,
                spansForLine = sampleSpans,
                parseErrorForLine = null,
                hoveredSegmentId = null,
                isDarkMode = true,
                onLineIndexChanged = {},
                onSegmentHovered = {}
            )
        }
    }
}
