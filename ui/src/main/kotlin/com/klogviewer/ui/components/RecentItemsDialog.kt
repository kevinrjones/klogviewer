package com.klogviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.repository.LocalFileSystem

@Composable
fun RecentItemsDialog(
    recentFiles: List<String>,
    recentDirectories: List<String>,
    localFileSystem: LocalFileSystem,
    onSelect: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearMissing: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    val hasMissing = recentFiles.any { !localFileSystem.exists(it) } || recentDirectories.any { !localFileSystem.exists(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recently Opened Items") },
        text = {
            val focusManager = LocalFocusManager.current
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .width(600.dp)
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                            focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                            true
                        } else {
                            false
                        }
                    }
            ) {
                if (hasMissing) {
                    Surface(
                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Some items no longer exist on disk.",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.error
                            )
                            TextButton(onClick = onClearMissing) {
                                Text(
                                    "Clear Missing",
                                    style = MaterialTheme.typography.caption,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (recentFiles.isNotEmpty()) {
                    Text(
                        "Files",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    recentFiles.forEachIndexed { index, path ->
                        RecentItemRow(
                            path, 
                            onSelect, 
                            onRemoveItem, 
                            isMissing = !localFileSystem.exists(path),
                            modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier
                        )
                    }
                }

                if (recentFiles.isNotEmpty() && recentDirectories.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (recentDirectories.isNotEmpty()) {
                    Text(
                        "Directories",
                        style = MaterialTheme.typography.subtitle2,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    recentDirectories.forEachIndexed { index, path ->
                        RecentItemRow(
                            path, 
                            onSelect, 
                            onRemoveItem, 
                            isMissing = !localFileSystem.exists(path),
                            modifier = if (index == 0 && recentFiles.isEmpty()) Modifier.focusRequester(focusRequester) else Modifier
                        )
                    }
                }

                if (recentFiles.isEmpty() && recentDirectories.isEmpty()) {
                    Text("No recent items found.", modifier = Modifier.padding(16.dp))
                }
            }
        },
        confirmButton = {
            val focusManager = LocalFocusManager.current
            Button(
                onClick = onDismiss,
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.key == Key.Tab && event.type == KeyEventType.KeyDown) {
                        focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                        true
                    } else {
                        false
                    }
                }
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RecentItemRow(
    path: String,
    onSelect: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    isMissing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onSelect(path) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = path,
                style = MaterialTheme.typography.body2.copy(
                    textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isMissing) MaterialTheme.colors.onSurface.copy(alpha = 0.4f) else MaterialTheme.colors.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(onClick = { onRemoveItem(path) }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from history",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
