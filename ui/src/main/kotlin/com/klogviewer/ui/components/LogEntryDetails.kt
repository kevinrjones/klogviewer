package com.klogviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.ui.mvi.LogEntryDetailViewMode

private const val MAX_DETAIL_VALUE_LENGTH = 10_000
private const val MAX_CONTENT_LENGTH = 50_000

@Composable
fun LogEntryDetails(
    entry: LogEntry?,
    onClose: () -> Unit,
    filterQueries: List<String> = emptyList(),
    isDarkMode: Boolean = false,
    showAnsiColors: Boolean = true,
    detailViewMode: LogEntryDetailViewMode = LogEntryDetailViewMode.STRUCTURED,
    expandedStructuredPaths: Set<String> = emptySet(),
    expandedStructuredScalarPaths: Set<String> = emptySet(),
    isRawPayloadExpanded: Boolean = false,
    onDetailViewModeChanged: (LogEntryDetailViewMode) -> Unit = {},
    onToggleStructuredPathExpansion: (String) -> Unit = {},
    onToggleStructuredScalarExpansion: (String) -> Unit = {},
    onRawPayloadExpansionChanged: (Boolean) -> Unit = {},
    onCopyPath: (String) -> Unit = {},
    onCopyValue: (String) -> Unit = {},
    onFilterByField: (String) -> Unit = {},
    onFilterByValue: (String, StructuredValue) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val inspectorActions = StructuredInspectorActions(
        onCopyPath = onCopyPath,
        onCopyValue = onCopyValue,
        onFilterByField = onFilterByField,
        onFilterByValue = onFilterByValue
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        elevation = 8.dp,
        color = MaterialTheme.colors.surface
    ) {
        if (entry == null) {
            emptyLogEntryDetails()
        } else {
            logEntryDetailsContent(
                entry = entry,
                onClose = onClose,
                filterQueries = filterQueries,
                isDarkMode = isDarkMode,
                showAnsiColors = showAnsiColors,
                detailViewMode = detailViewMode,
                expandedStructuredPaths = expandedStructuredPaths,
                expandedStructuredScalarPaths = expandedStructuredScalarPaths,
                isRawPayloadExpanded = isRawPayloadExpanded,
                onDetailViewModeChanged = onDetailViewModeChanged,
                onToggleStructuredPathExpansion = onToggleStructuredPathExpansion,
                onToggleStructuredScalarExpansion = onToggleStructuredScalarExpansion,
                onRawPayloadExpansionChanged = onRawPayloadExpansionChanged,
                inspectorActions = inspectorActions
            )
        }
    }
}

@Composable
private fun emptyLogEntryDetails() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Select a log entry to see details",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun logEntryDetailsContent(
    entry: LogEntry,
    onClose: () -> Unit,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    showAnsiColors: Boolean,
    detailViewMode: LogEntryDetailViewMode,
    expandedStructuredPaths: Set<String>,
    expandedStructuredScalarPaths: Set<String>,
    isRawPayloadExpanded: Boolean,
    onDetailViewModeChanged: (LogEntryDetailViewMode) -> Unit,
    onToggleStructuredPathExpansion: (String) -> Unit,
    onToggleStructuredScalarExpansion: (String) -> Unit,
    onRawPayloadExpansionChanged: (Boolean) -> Unit,
    inspectorActions: StructuredInspectorActions
) {
    val structuredData = entry.structuredData
    val rawPayload = structuredData?.rawPayload ?: entry.content.value
    val showStructured = structuredData != null && detailViewMode == LogEntryDetailViewMode.STRUCTURED

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        logEntryDetailsHeader(onClose = onClose)
        Spacer(modifier = Modifier.height(16.dp))

        entryMetadataSection(entry = entry)

        if (structuredData != null) {
            Spacer(modifier = Modifier.height(16.dp))
            detailViewModeToggle(
                mode = detailViewMode,
                onModeChanged = onDetailViewModeChanged
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (showStructured) {
            structuredInspector(
                root = structuredData.root,
                expandedStructuredPaths = expandedStructuredPaths,
                expandedStructuredScalarPaths = expandedStructuredScalarPaths,
                onTogglePathExpansion = onToggleStructuredPathExpansion,
                onToggleScalarExpansion = onToggleStructuredScalarExpansion,
                actions = inspectorActions
            )
        } else {
            rawPayloadSection(
                rawPayload = rawPayload,
                hasStructuredPayload = structuredData != null,
                filterQueries = filterQueries,
                isDarkMode = isDarkMode,
                showAnsiColors = showAnsiColors,
                isRawPayloadExpanded = isRawPayloadExpanded,
                onRawPayloadExpansionChanged = onRawPayloadExpansionChanged
            )
        }
    }
}

@Composable
private fun logEntryDetailsHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Log Entry Details", style = MaterialTheme.typography.h6)
        TooltipWrapper(tooltip = "Close details") {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close details")
            }
        }
    }
}

@Composable
private fun entryMetadataSection(entry: LogEntry) {
    DetailItem(label = "Timestamp", value = entry.timestamp.value)

    if (entry.level != LogLevel.UNKNOWN) {
        DetailItem(
            label = "Level",
            value = displayLevel(entry)
        )
    }

    entry.sourceId?.let { sourceId ->
        DetailItem(label = "Source", value = sourceId)
    }

    entry.fields.forEach { (key, value) ->
        if (isMetadataField(key)) {
            return@forEach
        }
        DetailItem(label = fieldLabel(key), value = value)
    }
}

private fun displayLevel(entry: LogEntry): String {
    return entry.fields["level"]
        ?: entry.level.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
}

private fun fieldLabel(rawKey: String): String {
    return rawKey
        .replace("_", " ")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
}

private fun isMetadataField(key: String): Boolean {
    return key == "timestamp" || key == "level" || key == "content"
}

@Composable
private fun detailViewModeToggle(
    mode: LogEntryDetailViewMode,
    onModeChanged: (LogEntryDetailViewMode) -> Unit
) {
    TabRow(
        selectedTabIndex = if (mode == LogEntryDetailViewMode.STRUCTURED) 0 else 1,
        backgroundColor = MaterialTheme.colors.surface,
        divider = {}
    ) {
        Tab(
            selected = mode == LogEntryDetailViewMode.STRUCTURED,
            onClick = { onModeChanged(LogEntryDetailViewMode.STRUCTURED) },
            text = { Text("Structured") }
        )
        Tab(
            selected = mode == LogEntryDetailViewMode.RAW,
            onClick = { onModeChanged(LogEntryDetailViewMode.RAW) },
            text = { Text("Raw") }
        )
    }
}

@Composable
private fun rawPayloadSection(
    rawPayload: String,
    hasStructuredPayload: Boolean,
    filterQueries: List<String>,
    isDarkMode: Boolean,
    showAnsiColors: Boolean,
    isRawPayloadExpanded: Boolean,
    onRawPayloadExpansionChanged: (Boolean) -> Unit
) {
    if (rawPayload.isBlank()) {
        return
    }

    Text(
        text = if (hasStructuredPayload) "Raw payload" else "Content",
        style = MaterialTheme.typography.subtitle2
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        val displayContent = if (isRawPayloadExpanded || rawPayload.length <= MAX_CONTENT_LENGTH) {
            rawPayload
        } else {
            rawPayload.take(MAX_CONTENT_LENGTH) +
                "\n... (truncated for performance, total length: ${rawPayload.length} chars)"
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = LogHighlighter.highlight(
                    text = displayContent,
                    filterQueries = filterQueries,
                    isDarkMode = isDarkMode,
                    showAnsiColors = showAnsiColors
                ),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.body2,
                fontFamily = FontFamily.Monospace
            )
            if (rawPayload.length > MAX_CONTENT_LENGTH) {
                TextButton(
                    onClick = { onRawPayloadExpansionChanged(!isRawPayloadExpanded) },
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                ) {
                    Text(if (isRawPayloadExpanded) "Show less" else "Show more")
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
        val displayValue = if (value.length > MAX_DETAIL_VALUE_LENGTH) {
            value.take(MAX_DETAIL_VALUE_LENGTH) + "... (truncated)"
        } else {
            value
        }
        Text(
            text = displayValue,
            style = MaterialTheme.typography.body2
        )
    }
}
