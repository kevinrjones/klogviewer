@file:Suppress("FunctionNaming")

package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.klogviewer.domain.model.StructuredValue

@Composable
fun StructuredInspector(
    root: StructuredValue,
    expandedStructuredPaths: Set<String>,
    expandedStructuredScalarPaths: Set<String>,
    onTogglePathExpansion: (String) -> Unit,
    onToggleScalarExpansion: (String) -> Unit,
    actions: StructuredInspectorActions
) {
    Text("Structured payload", style = MaterialTheme.typography.subtitle2)
    Spacer(modifier = Modifier.height(8.dp))
    StructuredValueNode(
        label = "root",
        path = "",
        value = root,
        depth = 0,
        expandedStructuredPaths = expandedStructuredPaths,
        expandedStructuredScalarPaths = expandedStructuredScalarPaths,
        onTogglePathExpansion = onTogglePathExpansion,
        onToggleScalarExpansion = onToggleScalarExpansion,
        actions = actions
    )
}

@Composable
private fun StructuredValueNode(
    label: String,
    path: String,
    value: StructuredValue,
    depth: Int,
    expandedStructuredPaths: Set<String>,
    expandedStructuredScalarPaths: Set<String>,
    onTogglePathExpansion: (String) -> Unit,
    onToggleScalarExpansion: (String) -> Unit,
    actions: StructuredInspectorActions
) {
    if (isContainerNode(value)) {
        ContainerNode(
            label = label,
            path = path,
            value = value,
            depth = depth,
            expandedStructuredPaths = expandedStructuredPaths,
            expandedStructuredScalarPaths = expandedStructuredScalarPaths,
            onTogglePathExpansion = onTogglePathExpansion,
            onToggleScalarExpansion = onToggleScalarExpansion,
            actions = actions
        )
    } else {
        ScalarNode(
            label = label,
            path = path,
            value = value,
            depth = depth,
            expandedStructuredScalarPaths = expandedStructuredScalarPaths,
            onToggleScalarExpansion = onToggleScalarExpansion,
            actions = actions
        )
    }
}

@Composable
private fun ContainerNode(
    label: String,
    path: String,
    value: StructuredValue,
    depth: Int,
    expandedStructuredPaths: Set<String>,
    expandedStructuredScalarPaths: Set<String>,
    onTogglePathExpansion: (String) -> Unit,
    onToggleScalarExpansion: (String) -> Unit,
    actions: StructuredInspectorActions
) {
    val isExpanded = path.isBlank() || expandedStructuredPaths.contains(path)

    ContainerNodeRow(
        label = label,
        path = path,
        isExpanded = isExpanded,
        depth = depth,
        value = value,
        onTogglePathExpansion = onTogglePathExpansion
    )
    StructuredNodeActionsRow(path = path, value = value, depth = depth, actions = actions)

    if (!isExpanded) {
        return
    }

    val children = structuredChildren(value, path)
    children
        .take(MAX_STRUCTURED_CHILDREN_PER_NODE)
        .forEach { child ->
            StructuredValueNode(
                label = child.label,
                path = child.path,
                value = child.value,
                depth = depth + 1,
                expandedStructuredPaths = expandedStructuredPaths,
                expandedStructuredScalarPaths = expandedStructuredScalarPaths,
                onTogglePathExpansion = onTogglePathExpansion,
                onToggleScalarExpansion = onToggleScalarExpansion,
                actions = actions
            )
        }

    if (children.size > MAX_STRUCTURED_CHILDREN_PER_NODE) {
        Text(
            text = "... ${children.size - MAX_STRUCTURED_CHILDREN_PER_NODE} additional items hidden",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = nodeStartPadding(depth + 1), top = 4.dp)
        )
    }
}

@Composable
private fun ContainerNodeRow(
    label: String,
    path: String,
    isExpanded: Boolean,
    depth: Int,
    value: StructuredValue,
    onTogglePathExpansion: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = nodeStartPadding(depth), top = 2.dp, bottom = 2.dp)
            .clickable(enabled = path.isNotBlank()) {
                onTogglePathExpansion(path)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (path.isBlank() || isExpanded) "▾" else "▸",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.width(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(8.dp))
        TypeCue(type = structuredTypeLabel(value))
    }
}

@Composable
private fun ScalarNode(
    label: String,
    path: String,
    value: StructuredValue,
    depth: Int,
    expandedStructuredScalarPaths: Set<String>,
    onToggleScalarExpansion: (String) -> Unit,
    actions: StructuredInspectorActions
) {
    val scalarValue = scalarDisplayValue(value)
    val isExpanded = expandedStructuredScalarPaths.contains(path)
    val isTruncated = scalarValue.length > MAX_STRUCTURED_SCALAR_PREVIEW_LENGTH
    val displayValue = if (isExpanded || !isTruncated) {
        scalarValue
    } else {
        scalarValue.take(MAX_STRUCTURED_SCALAR_PREVIEW_LENGTH) + "..."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = nodeStartPadding(depth), top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(8.dp))
        TypeCue(type = structuredTypeLabel(value))
    }
    Text(
        text = displayValue,
        style = MaterialTheme.typography.caption,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = nodeStartPadding(depth + 1), top = 2.dp)
    )
    if (isTruncated) {
        TextButton(
            onClick = { onToggleScalarExpansion(path) },
            modifier = Modifier.padding(start = nodeStartPadding(depth + 1))
        ) {
            Text(if (isExpanded) "Show less" else "Show more")
        }
    }

    StructuredNodeActionsRow(path = path, value = value, depth = depth, actions = actions)
}

@Composable
private fun StructuredNodeActionsRow(
    path: String,
    value: StructuredValue,
    depth: Int,
    actions: StructuredInspectorActions
) {
    if (path.isBlank()) {
        return
    }

    val isScalar = !isContainerNode(value)
    Row(
        modifier = Modifier.padding(start = nodeStartPadding(depth + 1)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TextButton(
            onClick = { actions.onCopyPath(path) },
            modifier = Modifier.testTag(copyPathActionTag(path))
        ) {
            Text("Copy path", style = MaterialTheme.typography.caption)
        }
        TextButton(
            onClick = { actions.onFilterByField(path) },
            modifier = Modifier.testTag(filterFieldActionTag(path))
        ) {
            Text("Filter field", style = MaterialTheme.typography.caption)
        }
        if (isScalar) {
            TextButton(
                onClick = { actions.onCopyValue(copyValueText(value)) },
                modifier = Modifier.testTag(copyValueActionTag(path))
            ) {
                Text("Copy value", style = MaterialTheme.typography.caption)
            }
            TextButton(
                onClick = { actions.onFilterByValue(path, value) },
                modifier = Modifier.testTag(filterValueActionTag(path))
            ) {
                Text("Filter value", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
private fun TypeCue(type: String) {
    Text(
        text = type,
        style = MaterialTheme.typography.overline,
        color = MaterialTheme.colors.onSurface.copy(alpha = 0.55f)
    )
}
