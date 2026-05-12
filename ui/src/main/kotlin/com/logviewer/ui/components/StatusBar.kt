package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(
    filePath: String,
    lineCount: Int,
    encoding: String = "UTF-8",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp),
        color = MaterialTheme.colors.primary,
        contentColor = MaterialTheme.colors.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (filePath.isEmpty()) "No file loaded" else filePath,
                style = MaterialTheme.typography.caption,
                maxLines = 1
            )
            Row {
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
