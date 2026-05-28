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

@Composable
fun FilterBar(
    filterQueries: List<String>,
    onAddQuery: (String) -> Unit,
    onRemoveQuery: (String) -> Unit,
    onClearQueries: () -> Unit,
    onOpenFileClick: () -> Unit,
    onOpenDirectoryClick: () -> Unit,
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
    onSplitClick: () -> Unit,
    timeFilterFrom: String,
    timeFilterTo: String,
    timeFilterPreset: TimeRangePreset?,
    timeFilterValidationMessage: String?,
    onTimeFilterFromChange: (String) -> Unit,
    onTimeFilterToChange: (String) -> Unit,
    onApplyTimeFilterPreset: (TimeRangePreset) -> Unit,
    onClearTimeFilter: () -> Unit,
    matchesCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }

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

            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))

            TimeFilterControls(
                fromValue = timeFilterFrom,
                toValue = timeFilterTo,
                preset = timeFilterPreset,
                validationMessage = timeFilterValidationMessage,
                onFromChange = onTimeFilterFromChange,
                onToChange = onTimeFilterToChange,
                onApplyPreset = onApplyTimeFilterPreset,
                onClear = onClearTimeFilter
            )

            Divider(modifier = Modifier.height(20.dp).width(1.dp).padding(horizontal = 4.dp))

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
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colors.onSurface),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (textState.isEmpty() && filterQueries.isEmpty()) {
                                    Text(
                                        text = "Filter...",
                                        style = MaterialTheme.typography.body2,
                                        fontSize = 14.sp,
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
private fun TimeFilterControls(
    fromValue: String,
    toValue: String,
    preset: TimeRangePreset?,
    validationMessage: String?,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onApplyPreset: (TimeRangePreset) -> Unit,
    onClear: () -> Unit
) {
    var presetMenuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        DateTimeFilterInput(
            value = fromValue,
            label = "From",
            testTag = "time_filter_from_input",
            clearTag = "time_filter_clear_from",
            clearTooltip = "Clear From",
            onValueChange = onFromChange
        )

        Spacer(modifier = Modifier.width(4.dp))

        DateTimeFilterInput(
            value = toValue,
            label = "To",
            testTag = "time_filter_to_input",
            clearTag = "time_filter_clear_to",
            clearTooltip = "Clear To",
            onValueChange = onToChange
        )

        Spacer(modifier = Modifier.width(4.dp))

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
                TimeRangePreset.entries.forEach { presetOption ->
                    DropdownMenuItem(onClick = {
                        presetMenuExpanded = false
                        onApplyPreset(presetOption)
                    }) {
                        Text(presetOption.displayLabel())
                    }
                }
            }
        }

        if (fromValue.isNotBlank() || toValue.isNotBlank() || preset != null) {
            FilterBarIcon(
                icon = Icons.Default.Clear,
                tooltip = "Clear time filter",
                onClick = onClear,
                testTag = "time_filter_clear"
            )
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

private const val TIME_FILTER_PLACEHOLDER = "YYYY-MM-DD HH:mm:ss"

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
private fun DateTimeFilterInput(
    value: String,
    label: String,
    testTag: String,
    clearTag: String,
    clearTooltip: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            Text(
                text = TIME_FILTER_PLACEHOLDER,
                style = MaterialTheme.typography.caption,
                fontSize = 12.sp
            )
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.caption.copy(fontSize = 12.sp),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        trailingIcon = {
            if (value.isNotBlank()) {
                TooltipWrapper(tooltip = clearTooltip) {
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(24.dp).testTag(clearTag)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = clearTooltip,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        },
        modifier = Modifier.width(200.dp).testTag(testTag),
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f),
            focusedIndicatorColor = MaterialTheme.colors.primary,
            unfocusedIndicatorColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
        )
    )
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
            Text(text = query, style = MaterialTheme.typography.caption, fontSize = 12.sp)
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
