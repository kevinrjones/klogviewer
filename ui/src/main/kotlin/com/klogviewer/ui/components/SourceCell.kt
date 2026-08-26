package com.klogviewer.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.LogEntry

@Composable
internal fun SourceCell(
    entry: LogEntry,
    sourceIds: List<String>,
    columnModifier: Modifier,
    logFontStyle: TextStyle
) {
    val displayNames = remember(sourceIds) { buildSourceDisplayNames(sourceIds) }
    val displayName = entry.sourceId?.let { displayNames[it] ?: it.extractSourceFileName() } ?: ""
    TooltipWrapper(
        tooltip = entry.sourceId ?: "",
        tooltipTestTag = "log_source_cell_tooltip"
    ) {
        Text(
            text = displayName,
            color = getSourceBadgeColor(entry.sourceId, sourceIds).takeIf { it != Color.Transparent }
                ?: MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            style = MaterialTheme.typography.caption.copy(
                fontFamily = logFontStyle.fontFamily,
                fontSize = logFontStyle.fontSize
            ),
            modifier = columnModifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

internal fun String?.extractSourceFileName(): String {
    if (this.isNullOrBlank()) return "Unknown Source"
    val normalized = this.removeSuffix("/").removeSuffix("\\")
    val fileName = normalized.substringAfterLast('/').substringAfterLast('\\')
    return fileName.ifBlank { normalized.ifBlank { "Unknown Source" } }
}

/**
 * Builds short display names for the `Source` column: file names by default,
 * disambiguated with the parent directory when two sources share a file name.
 */
internal fun buildSourceDisplayNames(sourceIds: List<String>): Map<String, String> {
    val fileNames = sourceIds.associateWith { it.extractSourceFileName() }
    val duplicated = fileNames.values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return fileNames.mapValues { (sourceId, fileName) ->
        if (fileName in duplicated) {
            val normalized = sourceId.removeSuffix("/").removeSuffix("\\")
            val parent = normalized.substringBeforeLast('/', "").substringAfterLast('/').substringAfterLast('\\')
            if (parent.isBlank()) fileName else "$parent/$fileName"
        } else {
            fileName
        }
    }
}

internal fun getEffectiveSourceIds(sourceIds: List<String>, logs: List<LogEntry>): List<String> {
    val entrySources = logs.mapNotNull { it.sourceId }.distinct().filter { it.isNotEmpty() }
    return when {
        entrySources.size > 1 -> entrySources
        sourceIds.size > 1 -> sourceIds
        else -> entrySources.ifEmpty { sourceIds }
    }
}
