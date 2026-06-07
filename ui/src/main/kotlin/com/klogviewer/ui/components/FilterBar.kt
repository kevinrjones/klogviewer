package com.klogviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.ui.mvi.TimeRangePreset

private val COMPACT_MENU_ITEM_HEIGHT = 28.dp
private val COMPACT_MENU_ITEM_HORIZONTAL_PADDING = 10.dp

private data class StructuredFilterOperator(
    val id: String,
    val token: String,
    val label: String,
    val requiresValue: Boolean = true
)

private val STRUCTURED_FILTER_OPERATORS = listOf(
    StructuredFilterOperator(id = "eq", token = "=", label = "Equals"),
    StructuredFilterOperator(id = "contains", token = "contains", label = "Contains"),
    StructuredFilterOperator(id = "regex", token = "~", label = "Regex"),
    StructuredFilterOperator(id = "gt", token = ">", label = "Greater than"),
    StructuredFilterOperator(id = "gte", token = ">=", label = "Greater than or equal"),
    StructuredFilterOperator(id = "lt", token = "<", label = "Less than"),
    StructuredFilterOperator(id = "lte", token = "<=", label = "Less than or equal"),
    StructuredFilterOperator(id = "exists", token = "exists", label = "Exists", requiresValue = false),
    StructuredFilterOperator(id = "missing", token = "missing", label = "Missing", requiresValue = false)
)

private val NUMERIC_VALUE_PATTERN = Regex("^-?\\d+(\\.\\d+)?$")

private fun buildStructuredFilterQuery(
    path: String,
    operator: StructuredFilterOperator,
    rawValue: String
): String {
    val normalizedPath = path.trim()
    if (!operator.requiresValue) {
        return "field:$normalizedPath ${operator.token}"
    }

    val valueToken = toStructuredFilterValueToken(rawValue.trim())
    return if (operator.token == "=") {
        "field:$normalizedPath=${valueToken}"
    } else {
        "field:$normalizedPath ${operator.token} $valueToken"
    }
}

private fun toStructuredFilterValueToken(value: String): String {
    val lowercaseValue = value.lowercase()
    if (lowercaseValue == "true" || lowercaseValue == "false" || lowercaseValue == "null") {
        return lowercaseValue
    }

    if (NUMERIC_VALUE_PATTERN.matches(value)) {
        return value
    }

    return "\"${escapeStructuredStringValue(value)}\""
}

private fun escapeStructuredStringValue(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

@Composable
private fun CompactMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = 112.dp)
            .height(COMPACT_MENU_ITEM_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = COMPACT_MENU_ITEM_HORIZONTAL_PADDING),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun FilterBar(
    filterQueries: List<String>,
    onAddQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearQueries: () -> Unit,
    onOpenFileClick: () -> Unit,
    onSftpClick: () -> Unit,
    onS3Click: () -> Unit,
    onAddFileClick: () -> Unit,
    onAddDirectoryClick: () -> Unit,
    onAddSftpClick: () -> Unit,
    onAddS3Click: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSidebar: () -> Unit,
    isReversed: Boolean,
    onToggleSortOrder: () -> Unit,
    isAutoScrollEnabled: Boolean,
    onToggleAutoScroll: () -> Unit,
    showAnsiColors: Boolean,
    onToggleAnsiColors: () -> Unit,
    isConnected: Boolean,
    onToggleConnection: () -> Unit,
    onRefresh: () -> Unit,
    onSplitClick: () -> Unit,
    timeFilterFrom: String,
    timeFilterTo: String,
    timeFilterPreset: TimeRangePreset?,
    timeFilterValidationMessage: String?,
    onApplyTimeFilterPreset: (TimeRangePreset) -> Unit,
    onClearTimeFilter: () -> Unit,
    matchesCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    var isStructuredFilterDialogOpen by remember { mutableStateOf(false) }
    var structuredFilterFieldPath by remember { mutableStateOf("") }
    var structuredFilterOperatorId by remember {
        mutableStateOf(STRUCTURED_FILTER_OPERATORS.first().id)
    }
    var structuredFilterValue by remember { mutableStateOf("") }

    val selectedStructuredOperator = STRUCTURED_FILTER_OPERATORS.firstOrNull {
        it.id == structuredFilterOperatorId
    } ?: STRUCTURED_FILTER_OPERATORS.first()

    val canApplyStructuredFilter = structuredFilterFieldPath.trim().isNotEmpty() &&
        (!selectedStructuredOperator.requiresValue || structuredFilterValue.trim().isNotEmpty())

    val closeStructuredFilterDialog = {
        isStructuredFilterDialogOpen = false
        structuredFilterFieldPath = ""
        structuredFilterOperatorId = STRUCTURED_FILTER_OPERATORS.first().id
        structuredFilterValue = ""
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("filter_bar"),
        elevation = 2.dp,
        color = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Actions
            FilterBarIcon(
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                tooltip = "Open Log File",
                onClick = onOpenFileClick,
                testTag = "toolbar_open_file"
            )
            FilterBarIcon(
                icon = Icons.Default.Cloud,
                tooltip = "Connect to SFTP",
                onClick = onSftpClick,
                testTag = "toolbar_connect_sftp"
            )
            FilterBarIcon(
                icon = Icons.Default.CloudQueue,
                tooltip = "Connect to S3",
                onClick = onS3Click,
                testTag = "toolbar_connect_s3"
            )

            Box {
                var menuExpanded by remember { mutableStateOf(false) }
                FilterBarIcon(
                    icon = Icons.Default.AddCircle, 
                    tooltip = "Add Logs to Workspace (Interleave)", 
                    onClick = { menuExpanded = true },
                    testTag = "add_file_to_workspace"
                )
                
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        onClick = {
                            menuExpanded = false
                            onAddFileClick()
                        },
                        modifier = Modifier.testTag("add_local_file_item")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Local File...")
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            menuExpanded = false
                            onAddDirectoryClick()
                        },
                        modifier = Modifier.testTag("add_local_directory_item")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Local Directory...")
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            menuExpanded = false
                            onAddSftpClick()
                        },
                        modifier = Modifier.testTag("add_remote_sftp_item")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Remote SFTP...")
                        }
                    }
                    DropdownMenuItem(
                        onClick = {
                            menuExpanded = false
                            onAddS3Click()
                        },
                        modifier = Modifier.testTag("add_remote_s3_item")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Remote S3...")
                        }
                    }
                }
            }
            
            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))
            
            // View Actions
            FilterBarIcon(icon = Icons.Default.Brightness4, tooltip = "Toggle Theme", onClick = onToggleTheme)
            FilterBarIcon(icon = Icons.AutoMirrored.Filled.ViewSidebar, tooltip = "Toggle Sidebar", onClick = onToggleSidebar)
            FilterBarIcon(icon = Icons.Default.VerticalSplit, tooltip = "Split Horizontal", onClick = onSplitClick)
            FilterBarIcon(
                icon = if (isReversed) Icons.Default.SwapVert else Icons.AutoMirrored.Filled.Sort,
                tooltip = if (isReversed) "Newest First" else "Oldest First",
                onClick = onToggleSortOrder
            )
            FilterBarIcon(
                icon = Icons.Default.ArrowDownward,
                tooltip = if (isAutoScrollEnabled) "Auto-scroll ON" else "Auto-scroll OFF",
                onClick = onToggleAutoScroll,
                tint = if (isAutoScrollEnabled) MaterialTheme.colors.primary else LocalContentColor.current
            )
            FilterBarIcon(
                icon = Icons.Default.Palette,
                tooltip = if (showAnsiColors) "ANSI Colors ON" else "ANSI Colors OFF",
                onClick = onToggleAnsiColors,
                tint = if (showAnsiColors) MaterialTheme.colors.primary else LocalContentColor.current
            )
            FilterBarIcon(
                icon = if (isConnected) Icons.Default.Link else Icons.Default.LinkOff,
                tooltip = if (isConnected) "Connected (Click to Disconnect)" else "Disconnected (Click to Connect)",
                onClick = onToggleConnection,
                tint = if (isConnected) MaterialTheme.colors.primary else Color.Gray
            )
            FilterBarIcon(
                icon = Icons.Default.Refresh,
                tooltip = "Refresh Sources",
                onClick = onRefresh,
                testTag = "toolbar_refresh"
            )

            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))

            TimeFilterControls(
                preset = timeFilterPreset,
                validationMessage = timeFilterValidationMessage,
                onApplyPreset = onApplyTimeFilterPreset,
                onClear = onClearTimeFilter
            )

            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))

            Box {
                FilterBarIcon(
                    icon = Icons.Default.Tune,
                    tooltip = "Add structured field filter",
                    onClick = { isStructuredFilterDialogOpen = true },
                    testTag = "structured_filter_trigger"
                )

                if (isStructuredFilterDialogOpen) {
                    StructuredFilterDialog(
                        fieldPath = structuredFilterFieldPath,
                        onFieldPathChange = { structuredFilterFieldPath = it },
                        operators = STRUCTURED_FILTER_OPERATORS,
                        selectedOperator = selectedStructuredOperator,
                        onOperatorSelected = { structuredFilterOperatorId = it.id },
                        value = structuredFilterValue,
                        onValueChange = { structuredFilterValue = it },
                        canApply = canApplyStructuredFilter,
                        onApply = {
                            val generatedQuery = buildStructuredFilterQuery(
                                path = structuredFilterFieldPath,
                                operator = selectedStructuredOperator,
                                rawValue = structuredFilterValue
                            )
                            onAddQuery(generatedQuery)
                            closeStructuredFilterDialog()
                        },
                        onCancel = closeStructuredFilterDialog
                    )
                }
            }

            // Search Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Filter Chips
                    filterQueries.forEach { query ->
                        FilterChip(query = query, onRemove = { onRemoveQuery(query) })
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Input field
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        modifier = Modifier.weight(1f).testTag("filter_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (textState.isNotBlank()) {
                                onAddQuery(textState)
                                textState = ""
                            }
                        }),
                        textStyle = MaterialTheme.typography.body2.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textState.isEmpty() && filterQueries.isEmpty()) {
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
                    
                    if (filterQueries.isNotEmpty() || textState.isNotEmpty()) {
                        TooltipWrapper(tooltip = "Clear all filters") {
                            IconButton(onClick = { 
                                onClearQueries()
                                textState = ""
                            }, modifier = Modifier.size(20.dp).testTag("clear_all_filters")) {
                                Icon(Icons.Default.Close, contentDescription = "Clear all filters", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Results count
            if (totalCount > 0) {
                Text(
                    text = "$matchesCount / $totalCount",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StructuredFilterDialog(
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

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { operatorMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("structured_filter_operator_trigger")
                    ) {
                        Text(selectedOperator.label)
                    }

                    DropdownMenu(
                        expanded = operatorMenuExpanded,
                        onDismissRequest = { operatorMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.95f)
                    ) {
                        operators.forEach { operator ->
                            DropdownMenuItem(
                                onClick = {
                                    operatorMenuExpanded = false
                                    onOperatorSelected(operator)
                                },
                                modifier = Modifier.testTag("structured_filter_operator_${operator.id}")
                            ) {
                                Text(operator.label)
                            }
                        }
                    }
                }

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
private fun TimeFilterControls(
    preset: TimeRangePreset?,
    validationMessage: String?,
    onApplyPreset: (TimeRangePreset) -> Unit,
    onClear: () -> Unit
) {
    var presetMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box {
            FilterBarIcon(
                icon = Icons.Default.Schedule,
                tooltip = preset?.let { "Preset: ${it.displayLabel()}" } ?: "Time range presets",
                onClick = { presetMenuExpanded = true },
                testTag = "time_filter_preset"
            )
            DropdownMenu(
                expanded = presetMenuExpanded,
                onDismissRequest = { presetMenuExpanded = false }
            ) {
                TimeRangePreset.entries
                    .filterNot { it == TimeRangePreset.FULL_LOADED_RANGE }
                    .forEach { presetOption ->
                        CompactMenuItem(
                            text = presetOption.displayLabel(),
                            onClick = {
                                presetMenuExpanded = false
                                onApplyPreset(presetOption)
                            },
                            modifier = Modifier.testTag("time_filter_preset_${presetOption.name.lowercase()}")
                        )
                    }

                Divider()

                CompactMenuItem(
                    text = "Reset",
                    onClick = {
                        presetMenuExpanded = false
                        onClear()
                    },
                    modifier = Modifier.testTag("time_filter_clear_menu_item")
                )
            }
        }

        validationMessage?.let {
            Spacer(modifier = Modifier.width(4.dp))
            TooltipWrapper(tooltip = it) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = it,
                    tint = MaterialTheme.colors.error,
                    modifier = Modifier.size(16.dp).testTag("time_filter_validation")
                )
            }
        }
    }
}

private fun TimeRangePreset.displayLabel(): String {
    return when (this) {
        TimeRangePreset.LAST_5_MINUTES -> "Last 5 minutes"
        TimeRangePreset.LAST_15_MINUTES -> "Last 15 minutes"
        TimeRangePreset.LAST_30_MINUTES -> "Last 30 minutes"
        TimeRangePreset.LAST_1_HOUR -> "Last 1 hour"
        TimeRangePreset.LAST_6_HOURS -> "Last 6 hours"
        TimeRangePreset.LAST_24_HOURS -> "Last 24 hours"
        TimeRangePreset.VISIBLE_WINDOW -> "Visible window"
        TimeRangePreset.FULL_LOADED_RANGE -> "Full loaded range"
        TimeRangePreset.CUSTOM -> "Custom"
    }
}

@Composable
private fun FilterBarIcon(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current,
    testTag: String? = null
) {
    val tag = testTag ?: tooltip.lowercase().replace(" ", "_")
    TooltipWrapper(tooltip = tooltip) {
        IconButton(onClick = onClick, modifier = Modifier.size(28.dp).testTag(tag)) {
            Icon(icon, contentDescription = tooltip, modifier = Modifier.size(18.dp), tint = tint)
        }
    }
}

@Composable
private fun FilterChip(
    query: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(text = query, style = MaterialTheme.typography.caption, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            TooltipWrapper(tooltip = "Remove filter") {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onRemove() }
                )
            }
        }
    }
}
