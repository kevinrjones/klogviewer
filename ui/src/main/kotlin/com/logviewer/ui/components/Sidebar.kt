package com.logviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.logviewer.domain.model.LogLevel

@Composable
fun Sidebar(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    levelFilters: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(if (isExpanded) 200.dp else 56.dp),
        elevation = 4.dp,
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = if (isExpanded) "Collapse Sidebar" else "Expand Sidebar"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SidebarItem(
                icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                label = if (isDarkMode) "Light Mode" else "Dark Mode",
                isExpanded = isExpanded,
                onClick = onToggleTheme
            )

            if (isExpanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Log Levels",
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                LogLevel.entries.forEach { level ->
                    LogLevelToggle(
                        level = level,
                        isEnabled = levelFilters.contains(level),
                        onToggle = { onToggleLevel(level) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isExpanded) {
                Text(
                    text = "v1.0.0",
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
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
        )
        Text(
            text = level.name,
            style = MaterialTheme.typography.body2,
            maxLines = 1
        )
    }
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label)
        if (isExpanded) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, maxLines = 1, style = MaterialTheme.typography.body1)
        }
    }
}
