package com.logviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logviewer.domain.model.LogEntry
import com.logviewer.domain.model.LogLevel
import com.logviewer.ui.theme.LogLevelColors
import com.logviewer.ui.theme.LogViewerTheme

@Composable
fun LogList(
    logs: List<LogEntry>,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    selectedEntry: LogEntry? = null,
    onEntryClick: (LogEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                ) {
                    LogListHeader()
                    LazyColumn(
                        state = verticalScrollState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(logs) { index, entry ->
                            LogEntryRow(
                                entry = entry,
                                lineNumber = index + 1,
                                filterQueries = filterQueries,
                                isDarkMode = isDarkMode,
                                isSelected = entry == selectedEntry,
                                onClick = { onEntryClick(entry) }
                            )
                        }
                    }
                }
                HorizontalScrollbar(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    adapter = rememberScrollbarAdapter(horizontalScrollState)
                )
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().width(8.dp),
                adapter = rememberScrollbarAdapter(verticalScrollState)
            )
        }
    }
}

@Composable
fun LogListHeader() {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
            )
            Text(
                text = "Timestamp",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(180.dp).padding(horizontal = 4.dp)
            )
            Text(
                text = "Level",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)
            )
            Text(
                text = "Message",
                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun LogEntryRow(
    entry: LogEntry,
    lineNumber: Int,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val logColors = LogViewerTheme.logColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple()
            )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Gutter / Line Number
        Text(
            text = lineNumber.toString().padStart(4, ' '),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
        )
        
        Text(
            text = entry.timestamp.value,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(180.dp).padding(horizontal = 4.dp)
        )
        Text(
            text = "[${entry.level}]",
            color = getLevelColor(entry.level, logColors),
            style = MaterialTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        val sourceId = entry.sourceId
        if (sourceId != null) {
            SourceBadge(sourceId = sourceId)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = LogHighlighter.highlight(entry.content.value, filterQueries, isDarkMode),
            style = MaterialTheme.typography.body1,
            fontSize = 12.sp,
            softWrap = false,
            overflow = TextOverflow.Visible
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
