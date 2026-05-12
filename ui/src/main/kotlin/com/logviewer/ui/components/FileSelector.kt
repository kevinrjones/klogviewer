package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FileSelector(
    path: String,
    onLoadClick: (String) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(path) { mutableStateOf(path) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Log File Path") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onBrowseClick,
            modifier = Modifier.height(56.dp)
        ) {
            Text("Browse")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onLoadClick(text) },
            modifier = Modifier.height(56.dp)
        ) {
            Text("Load")
        }
    }
}
