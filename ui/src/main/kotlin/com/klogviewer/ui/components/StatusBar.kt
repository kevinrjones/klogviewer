package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(
    filePath: String,
    lineCount: Int,
    encoding: String = "UTF-8",
    parserName: String? = null,
    availableParsers: List<String> = emptyList(),
    onParserSelect: (String) -> Unit = {},
    isMissing: Boolean = false,
    isConnected: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showParserMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        color = when {
            isMissing -> Color.Red
            !isConnected -> Color.Gray
            else -> MaterialTheme.colors.primary
        },
        contentColor = MaterialTheme.colors.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    filePath.isEmpty() -> "No file loaded"
                    !isConnected -> "$filePath (Disconnected)"
                    else -> filePath
                },
                style = MaterialTheme.typography.caption.copy(
                    textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1
            )
            Row {
                if (parserName != null) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showParserMenu = true }
                        ) {
                            Text(
                                text = "Format: $parserName",
                                style = MaterialTheme.typography.caption
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showParserMenu,
                            onDismissRequest = { showParserMenu = false }
                        ) {
                            availableParsers.forEach { name ->
                                DropdownMenuItem(onClick = {
                                    onParserSelect(name)
                                    showParserMenu = false
                                }) {
                                    Text(name, style = MaterialTheme.typography.caption)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = "Lines: $lineCount",
                    style = MaterialTheme.typography.caption
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = encoding,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
