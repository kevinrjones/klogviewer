package com.klogviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.LogLevel

@Composable
fun Sidebar(
    isExpanded: Boolean,
    levelFilters: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    onToggleAllLevels: () -> Unit,
    levelCounts: Map<LogLevel, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isExpanded) 200.dp else 56.dp)
            .testTag("sidebar"),
        elevation = 4.dp,
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            if (isExpanded) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FILTERS",
                        style = MaterialTheme.typography.overline,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Levels (${LogLevel.entries.size})",
                        style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }

                LogLevelToggle(
                    label = "All",
                    isEnabled = levelFilters.size == LogLevel.entries.size,
                    count = levelCounts.values.sum(),
                    onToggle = onToggleAllLevels
                )

                LogLevel.entries.forEach { level ->
                    LogLevelToggle(
                        level = level,
                        isEnabled = levelFilters.contains(level),
                        count = levelCounts[level] ?: 0,
                        onToggle = { onToggleLevel(level) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isExpanded) {
                Text(
                    text = "v1.3.0",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun LogLevelToggle(
    level: LogLevel,
    isEnabled: Boolean,
    count: Int,
    onToggle: () -> Unit
) {
    LogLevelToggle(
        label = level.name.lowercase().replaceFirstChar { it.uppercase() },
        isEnabled = isEnabled,
        count = count,
        onToggle = onToggle
    )
}

@Composable
private fun LogLevelToggle(
    label: String,
    isEnabled: Boolean,
    count: Int,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp)
            .padding(start = 24.dp, end = 8.dp)
            .testTag("level_toggle_$label"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Custom square checkbox to match the requested style
        Box(
            modifier = Modifier
                .size(14.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
                .background(
                    if (isEnabled) MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

