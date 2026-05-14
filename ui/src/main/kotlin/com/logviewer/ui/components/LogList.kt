package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel
import com.logviewer.ui.theme.LogLevelColors
import com.logviewer.ui.theme.LogViewerTheme

@Composable
fun LogList(
    logs: List<LogEntry>,
    searchQuery: String,
    isDarkMode: Boolean,
    selectedEntry: LogEntry? = null,
    onEntryClick: (LogEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(logs) { index, entry ->
            LogEntryRow(
                entry = entry,
                lineNumber = index + 1,
                searchQuery = searchQuery,
                isDarkMode = isDarkMode,
                isSelected = entry == selectedEntry,
                onClick = { onEntryClick(entry) }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LogEntryRow(
    entry: LogEntry,
    lineNumber: Int,
    searchQuery: String,
    isDarkMode: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val logColors = LogViewerTheme.logColors
    Surface(
        color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 1.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
        // Gutter / Line Number
        Text(
            text = lineNumber.toString().padStart(4, ' '),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(40.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = entry.timestamp.value,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(150.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "[${entry.level}]",
            color = getLevelColor(entry.level, logColors),
            style = MaterialTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            modifier = Modifier.width(70.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        val sourceId = entry.sourceId
        if (sourceId != null) {
            SourceBadge(sourceId = sourceId)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = LogHighlighter.highlight(entry.content.value, searchQuery, isDarkMode),
            style = MaterialTheme.typography.body1,
            fontSize = 12.sp
        )
    }
}
}

private fun getLevelColor(level: LogLevel, colors: LogLevelColors): Color = when (level) {
    LogLevel.DEBUG -> colors.debug
    LogLevel.INFO -> colors.info
    LogLevel.WARN -> colors.warn
    LogLevel.ERROR -> colors.error
    LogLevel.FATAL -> colors.fatal
    LogLevel.UNKNOWN -> colors.unknown
}
