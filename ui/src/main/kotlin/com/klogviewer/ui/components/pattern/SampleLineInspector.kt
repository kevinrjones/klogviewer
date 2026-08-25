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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header / Carousel Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sample Line Inspector",
                    style = MaterialTheme.typography.titleSmall
                )

                if (sampleLines.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onLineIndexChanged(selectedLineIndex - 1) },
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
                            onClick = { onLineIndexChanged(selectedLineIndex + 1) },
                            enabled = selectedLineIndex < sampleLines.size - 1,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Line")
                        }
                    }
                }
            }

            // Monospace Line Viewer
            val currentLine = sampleLines.getOrNull(selectedLineIndex) ?: "No sample lines available"

            SelectionContainer {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (parseErrorForLine != null) 1.5.dp else 1.dp,
                            color = if (parseErrorForLine != null) Color(0xFFE74C3C) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(12.dp)
                ) {
                    val annotatedString = buildAnnotatedString {
                        if (spansForLine.isEmpty()) {
                            append(currentLine)
                        } else {
                            var lastIdx = 0
                            val sortedSpans = spansForLine.sortedBy { it.range.first }
                            for (span in sortedSpans) {
                                if (span.range.first > lastIdx && lastIdx < currentLine.length) {
                                    append(currentLine.substring(lastIdx, span.range.first.coerceAtMost(currentLine.length)))
                                }
                                val spanText = currentLine.substring(
                                    span.range.first.coerceIn(0, currentLine.length),
                                    (span.range.last + 1).coerceIn(0, currentLine.length)
                                )
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
                                lastIdx = span.range.last + 1
                            }
                            if (lastIdx < currentLine.length) {
                                append(currentLine.substring(lastIdx))
                            }
                        }
                    }

                    Text(
                        text = annotatedString,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            // Error Annotation Banner
            if (parseErrorForLine != null) {
                Surface(
                    color = Color(0xFFE74C3C).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ Parse Error at offset ${parseErrorForLine.errorOffset}: ${parseErrorForLine.message}",
                            style = TextStyle(
                                color = Color(0xFFE74C3C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
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
