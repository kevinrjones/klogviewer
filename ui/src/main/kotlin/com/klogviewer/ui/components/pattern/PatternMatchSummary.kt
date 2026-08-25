package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.PatternParseError

@Composable
fun PatternMatchSummary(
    matchedCount: Int,
    totalCount: Int,
    confidenceScore: Float,
    parseErrors: List<PatternParseError>,
    isDiagnosticsDrawerOpen: Boolean,
    onToggleDiagnosticsDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAllMatched = totalCount > 0 && matchedCount == totalCount
    val badgeBgColor = if (isAllMatched) Color(0xFF2ECC71).copy(alpha = 0.2f) else Color(0xFFF39C12).copy(alpha = 0.2f)
    val badgeTextColor = if (isAllMatched) Color(0xFF2ECC71) else Color(0xFFF39C12)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isAllMatched) "✓ $matchedCount/$totalCount sample lines matched (100%)"
                        else "⚠️ $matchedCount/$totalCount lines matched (${totalCount - matchedCount} errors)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = badgeTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (parseErrors.isNotEmpty()) {
                    TextButton(onClick = onToggleDiagnosticsDrawer) {
                        Text(if (isDiagnosticsDrawerOpen) "Hide Diagnostics" else "Diagnostics (${parseErrors.size})")
                    }
                }
            }

            // Diagnostics Drawer
            if (isDiagnosticsDrawerOpen && parseErrors.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
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
                                    color = Color(0xFFE74C3C),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

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
