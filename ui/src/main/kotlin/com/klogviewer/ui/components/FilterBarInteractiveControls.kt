package com.klogviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val STRUCTURED_FILTER_OPERATOR_MENU_WIDTH_FRACTION = 0.95f
private val compactMenuItemHeight = 28.dp
private val compactMenuItemHorizontalPadding = 10.dp

@Composable
internal fun compactMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 112.dp)
            .height(compactMenuItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = compactMenuItemHorizontalPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, style = MaterialTheme.typography.body2)
    }
}

@Composable
internal fun workspaceAddActions(
    onAddFileClick: () -> Unit,
    onAddDirectoryClick: () -> Unit,
    onAddSftpClick: () -> Unit,
    onAddS3Click: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        filterBarIcon(
            icon = Icons.Default.AddCircle,
            tooltip = "Add Logs to Workspace (Interleave)",
            onClick = { menuExpanded = true },
            testTag = "add_file_to_workspace"
        )

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            workspaceMenuItem(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                text = "Add Local File...",
                testTag = "add_local_file_item",
                onSelect = onAddFileClick,
                onDismiss = { menuExpanded = false }
            )
            workspaceMenuItem(
                icon = Icons.Default.Folder,
                text = "Add Local Directory...",
                testTag = "add_local_directory_item",
                onSelect = onAddDirectoryClick,
                onDismiss = { menuExpanded = false }
            )
            workspaceMenuItem(
                icon = Icons.Default.Cloud,
                text = "Add Remote SFTP...",
                testTag = "add_remote_sftp_item",
                onSelect = onAddSftpClick,
                onDismiss = { menuExpanded = false }
            )
            workspaceMenuItem(
                icon = Icons.Default.CloudQueue,
                text = "Add Remote S3...",
                testTag = "add_remote_s3_item",
                onSelect = onAddS3Click,
                onDismiss = { menuExpanded = false }
            )
        }
    }
}

@Composable
private fun workspaceMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    testTag: String,
    onSelect: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        onClick = {
            onDismiss()
            onSelect()
        },
        modifier = Modifier.testTag(testTag)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
    }
}

@Composable
internal fun structuredFilterActions(
    draft: StructuredFilterDraftState,
    selectedOperator: StructuredFilterOperator,
    canApply: Boolean,
    isDialogOpen: Boolean,
    onOpen: () -> Unit,
    onDraftChange: (StructuredFilterDraftState) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Box {
        filterBarIcon(
            icon = Icons.Default.Tune,
            tooltip = "Add structured field filter",
            onClick = onOpen,
            testTag = "structured_filter_trigger"
        )

        if (isDialogOpen) {
            structuredFilterDialog(
                fieldPath = draft.fieldPath,
                onFieldPathChange = { value -> onDraftChange(draft.copy(fieldPath = value)) },
                operators = STRUCTURED_FILTER_OPERATORS,
                selectedOperator = selectedOperator,
                onOperatorSelected = { operator ->
                    onDraftChange(draft.copy(operatorId = operator.id))
                },
                value = draft.value,
                onValueChange = { value -> onDraftChange(draft.copy(value = value)) },
                canApply = canApply,
                onApply = onApply,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun structuredFilterDialog(
    fieldPath: String,
    onFieldPathChange: (String) -> Unit,
    operators: List<StructuredFilterOperator>,
    selectedOperator: StructuredFilterOperator,
    onOperatorSelected: (StructuredFilterOperator) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    canApply: Boolean,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    var operatorMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Structured Filter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fieldPath,
                    onValueChange = onFieldPathChange,
                    label = { Text("Field / Path") },
                    placeholder = { Text("Properties.UserId") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("structured_filter_field_input")
                )

                structuredFilterOperatorSelector(
                    selectedOperator = selectedOperator,
                    operators = operators,
                    operatorMenuExpanded = operatorMenuExpanded,
                    onExpandChanged = { operatorMenuExpanded = it },
                    onOperatorSelected = onOperatorSelected
                )

                if (selectedOperator.requiresValue) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("Value") },
                        placeholder = { Text("timeout") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("structured_filter_value_input")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onApply,
                enabled = canApply,
                modifier = Modifier.testTag("structured_filter_apply")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag("structured_filter_cancel")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun structuredFilterOperatorSelector(
    selectedOperator: StructuredFilterOperator,
    operators: List<StructuredFilterOperator>,
    operatorMenuExpanded: Boolean,
    onExpandChanged: (Boolean) -> Unit,
    onOperatorSelected: (StructuredFilterOperator) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { onExpandChanged(true) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("structured_filter_operator_trigger")
        ) {
            Text(selectedOperator.label)
        }

        DropdownMenu(
            expanded = operatorMenuExpanded,
            onDismissRequest = { onExpandChanged(false) },
            modifier = Modifier.fillMaxWidth(STRUCTURED_FILTER_OPERATOR_MENU_WIDTH_FRACTION)
        ) {
            operators.forEach { operator ->
                DropdownMenuItem(
                    onClick = {
                        onExpandChanged(false)
                        onOperatorSelected(operator)
                    },
                    modifier = Modifier.testTag("structured_filter_operator_${operator.id}")
                ) {
                    Text(operator.label)
                }
            }
        }
    }
}

@Composable
internal fun filterQueryInputArea(
    modifier: Modifier = Modifier,
    filterQueries: List<String>,
    text: String,
    onTextChange: (String) -> Unit,
    onAddQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearQueries: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            filterQueries.forEach { query ->
                filterChip(query = query, onRemove = { onRemoveQuery(query) })
                Spacer(modifier = Modifier.width(4.dp))
            }

            filterInputField(
                filterQueries = filterQueries,
                text = text,
                onTextChange = onTextChange,
                onAddQuery = onAddQuery
            )

            val canClear = filterQueries.isNotEmpty() || text.isNotEmpty()
            if (canClear) {
                clearFiltersButton(onClearQueries = onClearQueries)
            }
        }
    }
}

@Composable
private fun RowScope.filterInputField(
    filterQueries: List<String>,
    text: String,
    onTextChange: (String) -> Unit,
    onAddQuery: (String) -> Unit
) {
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .weight(1f)
            .testTag("filter_input"),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            if (text.isNotBlank()) {
                onAddQuery(text)
                onTextChange("")
            }
        }),
        textStyle = MaterialTheme.typography.body2.copy(
            fontSize = 13.sp,
            color = MaterialTheme.colors.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty() && filterQueries.isEmpty()) {
                    Text(
                        text = "Filter...",
                        style = MaterialTheme.typography.body2,
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun clearFiltersButton(onClearQueries: () -> Unit) {
    TooltipWrapper(tooltip = "Clear all filters") {
        IconButton(
            onClick = onClearQueries,
            modifier = Modifier
                .size(20.dp)
                .testTag("clear_all_filters")
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear all filters",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
internal fun resultsCount(matchesCount: Int, totalCount: Int) {
    if (totalCount > 0) {
        Text(
            text = "$matchesCount / $totalCount",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}
