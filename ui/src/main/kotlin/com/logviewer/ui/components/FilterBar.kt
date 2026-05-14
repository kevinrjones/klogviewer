package com.logviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterBar(
    filterQueries: List<String>,
    onAddQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearQueries: () -> Unit,
    onAddClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSidebar: () -> Unit,
    isReversed: Boolean,
    onToggleSortOrder: () -> Unit,
    matchesCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        elevation = 2.dp,
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Actions
            SearchBarIcon(icon = Icons.Default.AddCircle, tooltip = "Add File to Workspace", onClick = onAddClick)
            
            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))
            
            // View Actions
            SearchBarIcon(icon = Icons.Default.Brightness4, tooltip = "Toggle Theme", onClick = onToggleTheme)
            SearchBarIcon(icon = Icons.AutoMirrored.Filled.ViewSidebar, tooltip = "Toggle Sidebar", onClick = onToggleSidebar)
            SearchBarIcon(
                icon = if (isReversed) Icons.Default.SwapVert else Icons.AutoMirrored.Filled.Sort,
                tooltip = if (isReversed) "Newest First" else "Oldest First",
                onClick = onToggleSortOrder
            )

            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))

            // Search Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Filter Chips
                    filterQueries.forEach { query ->
                        FilterChip(query = query, onRemove = { onRemoveQuery(query) })
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Input field
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (textState.isNotBlank()) {
                                onAddQuery(textState)
                                textState = ""
                            }
                        }),
                        textStyle = MaterialTheme.typography.body2.copy(
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textState.isEmpty() && filterQueries.isEmpty()) {
                                    Text(
                                        text = "Filter...",
                                        style = MaterialTheme.typography.body2,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    
                    if (filterQueries.isNotEmpty() || textState.isNotEmpty()) {
                        IconButton(onClick = { 
                            onClearQueries()
                            textState = ""
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear filters", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Results count
            if (totalCount > 0) {
                Text(
                    text = "$matchesCount / $totalCount",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBarIcon(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, contentDescription = tooltip, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterChip(
    query: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(text = query, style = MaterialTheme.typography.caption, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
