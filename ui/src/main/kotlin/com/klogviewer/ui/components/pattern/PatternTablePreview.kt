package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.klogviewer.domain.model.PreviewTableRow

@Composable
fun PatternTablePreview(
    columns: List<String>,
    rows: List<PreviewTableRow>,
    hoveredColumnName: String?,
    isDarkMode: Boolean,
    onColumnHovered: (String?) -> Unit,
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
            Text(
                text = "Live Table Grid Preview",
                style = MaterialTheme.typography.titleSmall
            )

            if (columns.isEmpty()) {
                Text(
                    text = "No columns mapped yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    // Column Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columns.forEach { colName ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isHovered by interactionSource.collectIsHoveredAsState()

                            LaunchedEffect(isHovered) {
                                if (isHovered) onColumnHovered(colName) else if (hoveredColumnName == colName) onColumnHovered(null)
                            }

                            val isColumnHighlighted = hoveredColumnName.equals(colName, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .hoverable(interactionSource)
                                    .background(
                                        if (isColumnHighlighted) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = colName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isColumnHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Data Rows
                    rows.forEachIndexed { index, row ->
                        val rowBg = if (index % 2 == 0) {
                            if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFAFAFA)
                        } else {
                            if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowBg)
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            columns.forEach { colName ->
                                val value = row.fields[colName] ?: ""
                                val isColumnHighlighted = hoveredColumnName.equals(colName, ignoreCase = true)

                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (colName.equals("level", ignoreCase = true)) {
                                        LevelBadge(levelStr = value, isDarkMode = isDarkMode)
                                    } else {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = if (isColumnHighlighted) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isColumnHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelBadge(levelStr: String, isDarkMode: Boolean) {
    val (bgColor, textColor) = when (levelStr.uppercase()) {
        "ERROR", "FATAL", "SEVERE" -> Color(0xFFE74C3C).copy(alpha = 0.25f) to Color(0xFFE74C3C)
        "WARN", "WARNING" -> Color(0xFFF39C12).copy(alpha = 0.25f) to Color(0xFFF39C12)
        "INFO" -> Color(0xFF2ECC71).copy(alpha = 0.25f) to Color(0xFF2ECC71)
        "DEBUG", "TRACE" -> Color(0xFF3498DB).copy(alpha = 0.25f) to Color(0xFF3498DB)
        else -> Color.Gray.copy(alpha = 0.2f) to Color.Gray
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = levelStr,
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Preview
@Composable
fun PatternTablePreviewPreview() {
    val cols = listOf("timestamp", "level", "thread", "message")
    val previewRows = listOf(
        PreviewTableRow(0, mapOf("timestamp" to "2026-08-25 10:00:00", "level" to "INFO", "thread" to "main", "message" to "Service booted")),
        PreviewTableRow(1, mapOf("timestamp" to "2026-08-25 10:00:01", "level" to "WARN", "thread" to "pool-1", "message" to "High memory usage"))
    )

    MaterialTheme {
        Surface {
            PatternTablePreview(
                columns = cols,
                rows = previewRows,
                hoveredColumnName = null,
                isDarkMode = true,
                onColumnHovered = {}
            )
        }
    }
}
