package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel

@Composable
fun LogEntryDetails(
    entry: LogEntry?,
    onClose: () -> Unit,
    filterQueries: List<String> = emptyList(),
    isDarkMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        elevation = 8.dp,
        color = MaterialTheme.colors.surface
    ) {
        if (entry == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Select a log entry to see details", style = MaterialTheme.typography.body2, color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Log Entry Details", style = MaterialTheme.typography.h6)
                    TooltipWrapper(tooltip = "Close details") {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close details")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                DetailItem(label = "Timestamp", value = entry.timestamp.value)
                if (entry.level != LogLevel.UNKNOWN) {
                    val displayLevel = entry.fields["level"] ?: entry.level.name.lowercase().replaceFirstChar { it.uppercase() }
                    DetailItem(label = "Level", value = displayLevel)
                }
                if (entry.sourceId != null) {
                    DetailItem(label = "Source", value = entry.sourceId!!)
                }

                entry.fields.forEach { (key, value) ->
                    if (key != "timestamp" && key != "level" && key != "content") {
                        val label = key.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                        DetailItem(label = label, value = value)
                    }
                }
                
                if (entry.content.value.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Content", style = MaterialTheme.typography.subtitle2)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = LogHighlighter.highlight(entry.content.value, filterQueries, isDarkMode),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.body2,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2
        )
    }
}
