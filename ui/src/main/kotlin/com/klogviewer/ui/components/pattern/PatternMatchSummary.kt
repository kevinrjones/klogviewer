package com.klogviewer.ui.components.pattern

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.PatternParseError
import com.klogviewer.ui.theme.KLogViewerTheme

@Composable
fun PatternMatchSummary(
    matchedCount: Int,
    totalCount: Int,
    confidenceScore: Float,
    parseErrors: List<PatternParseError>,
    isDiagnosticsDrawerOpen: Boolean,
    onToggleDiagnosticsDrawer: () -> Unit,
    showMissingTimestampWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isAllMatched = totalCount > 0 && matchedCount == totalCount
    val logColors = KLogViewerTheme.logColors
    val badgeTextColor = if (isAllMatched) logColors.info else logColors.warn
    val badgeBgColor = badgeTextColor.copy(alpha = 0.2f)

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Match Health Badge
                Surface(
                    color = badgeBgColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAllMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (isAllMatched) {
                                "All sample lines matched"
                            } else {
                                "Some sample lines failed"
                            },
                            tint = badgeTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAllMatched) "$matchedCount/$totalCount sample lines matched (100%)"
                            else "$matchedCount/$totalCount lines matched (${totalCount - matchedCount} errors)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = badgeTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (parseErrors.isNotEmpty()) {
                    TextButton(onClick = onToggleDiagnosticsDrawer) {
                        Text(if (isDiagnosticsDrawerOpen) "Hide Diagnostics" else "Diagnostics (${parseErrors.size})")
                    }
                }
            }

            if (showMissingTimestampWarning) {
                Surface(
                    color = logColors.warn.copy(alpha = WARNING_BACKGROUND_ALPHA),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Timestamp mapping warning",
                            tint = logColors.warn,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "No timestamp field mapped — interleaving with other sources will be approximate.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = logColors.warn
                            )
                        )
                    }
                }
            }

            // Diagnostics Drawer
            if (isDiagnosticsDrawerOpen && parseErrors.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Parse Errors:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        parseErrors.forEach { err ->
                            Text(
                                text = "• Line ${err.lineIndex + 1} (offset ${err.errorOffset}): ${err.message}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = logColors.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val WARNING_BACKGROUND_ALPHA = 0.15f

@Preview
@Composable
fun PatternMatchSummaryPreview() {
    val errs = listOf(
        PatternParseError(1, "2026-08-25 [bad]", 15, "Expected delimiter ']' at index 15")
    )

    MaterialTheme {
        Surface {
            PatternMatchSummary(
                matchedCount = 9,
                totalCount = 10,
                confidenceScore = 0.9f,
                parseErrors = errs,
                isDiagnosticsDrawerOpen = true,
                onToggleDiagnosticsDrawer = {}
            )
        }
    }
}
