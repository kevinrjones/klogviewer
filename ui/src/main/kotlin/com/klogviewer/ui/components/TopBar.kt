package com.klogviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    filePath: String,
    onLoadClick: (String) -> Unit,
    onAddToWorkspaceClick: (String) -> Unit,
    onBrowseClick: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchesCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        elevation = 4.dp,
        color = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            FileSelector(
                path = filePath,
                onLoadClick = onLoadClick,
                onAddToWorkspaceClick = onAddToWorkspaceClick,
                onBrowseClick = onBrowseClick
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "$matchesCount / $totalCount",
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.body1
            )
        }
    }
}
