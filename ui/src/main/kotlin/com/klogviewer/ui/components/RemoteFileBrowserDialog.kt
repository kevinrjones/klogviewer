package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.RemoteFile

@Composable
fun RemoteFileBrowserDialog(
    files: List<RemoteFile>,
    currentPath: String,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onSelectFiles: (List<String>) -> Unit,
    onSelectDirectory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Browse Remote Files") },
        text = {
            Column(modifier = Modifier.width(500.dp).height(400.dp)) {
                Text("Current Path: $currentPath", style = MaterialTheme.typography.caption)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        // Parent directory navigation
                        if (currentPath != "/" && currentPath != "") {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        val parent = currentPath.substringBeforeLast('/').ifEmpty { "/" }
                                        onNavigate(parent)
                                    }.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("..")
                                }
                            }
                        }

                        items(files.sortedBy { !it.isDirectory }) { file ->
                            val isSelected = selectedPaths.contains(file.path)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (file.isDirectory) {
                                            onNavigate(file.path)
                                        } else {
                                            selectedPaths = if (isSelected) {
                                                selectedPaths - file.path
                                            } else {
                                                selectedPaths + file.path
                                            }
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (file.isDirectory) {
                                    Icon(Icons.Default.Folder, contentDescription = "Folder", tint = Color(0xFFFFA000))
                                } else {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedPaths = if (checked == true) {
                                                selectedPaths + file.path
                                            } else {
                                                selectedPaths - file.path
                                            }
                                        }
                                    )
                                    Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = "File", tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(file.name, modifier = Modifier.weight(1f))
                                if (!file.isDirectory) {
                                    Text("${file.size / 1024} KB", style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = { onSelectDirectory(currentPath) },
                    enabled = !isLoading
                ) {
                    Text("Select This Directory")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSelectFiles(selectedPaths.toList()) },
                    enabled = !isLoading && selectedPaths.isNotEmpty()
                ) {
                    Text("Select ${selectedPaths.size} Files")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
