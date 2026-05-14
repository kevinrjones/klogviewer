package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logviewer.domain.model.LogLevel

@Composable
fun RibbonBar(
    onOpenClick: () -> Unit,
    onAddClick: () -> Unit,
    onClearClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSidebar: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchesCount: Int,
    totalCount: Int,
    levelFilters: Set<LogLevel>,
    onToggleLevel: (LogLevel) -> Unit,
    isReversed: Boolean,
    onToggleSortOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        color = MaterialTheme.colors.surface
    ) {
        Column {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RibbonGroup(label = "File") {
                    RibbonButton(icon = Icons.Default.FolderOpen, label = "Open", onClick = onOpenClick)
                    RibbonButton(icon = Icons.Default.AddCircle, label = "Add", onClick = onAddClick)
                    RibbonButton(icon = Icons.Default.Delete, label = "Clear", onClick = onClearClick)
                }
                
                Divider(modifier = Modifier.height(60.dp).width(1.dp).padding(vertical = 4.dp))
                
                RibbonGroup(label = "View") {
                    RibbonButton(
                        icon = Icons.Default.Brightness4, 
                        label = "Theme", 
                        onClick = onToggleTheme
                    )
                    RibbonButton(
                        icon = Icons.Default.ViewSidebar, 
                        label = "Sidebar", 
                        onClick = onToggleSidebar
                    )
                    RibbonButton(
                        icon = if (isReversed) Icons.Default.SwapVert else Icons.Default.Sort,
                        label = if (isReversed) "Newest First" else "Oldest First",
                        onClick = onToggleSortOrder
                    )
                }
                
                Divider(modifier = Modifier.height(60.dp).width(1.dp).padding(vertical = 4.dp))
                
                RibbonGroup(label = "Filters") {
                    LogLevel.entries.forEach { level ->
                        val isSelected = levelFilters.contains(level)
                        RibbonToggleButton(
                            label = level.name,
                            isSelected = isSelected,
                            onToggle = { onToggleLevel(level) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Search Area
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.width(300.dp).padding(end = 8.dp),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "$matchesCount / $totalCount",
                                style = MaterialTheme.typography.caption
                            )
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Composable
private fun RibbonGroup(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun RibbonButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
            Text(label, fontSize = 10.sp)
        }
    }
}

@Composable
private fun RibbonToggleButton(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    TextButton(
        onClick = onToggle,
        colors = if (isSelected) ButtonDefaults.textButtonColors(backgroundColor = MaterialTheme.colors.primary.copy(alpha = 0.1f)) else ButtonDefaults.textButtonColors(),
        modifier = Modifier.padding(horizontal = 2.dp).height(32.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
        )
    }
}

