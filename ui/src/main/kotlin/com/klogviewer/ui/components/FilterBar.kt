package com.klogviewer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.klogviewer.ui.mvi.TimeRangePreset
import com.klogviewer.ui.theme.KLogViewerTheme

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
    onEditPatternMapping: (() -> Unit)? = null,
    timeFilterFrom: String,
    timeFilterTo: String,
    timeFilterPreset: TimeRangePreset?,
    timeFilterValidationMessage: String?,
    onApplyTimeFilterPreset: (TimeRangePreset) -> Unit,
    onClearTimeFilter: () -> Unit,
    matchesCount: Int,
    totalCount: Int,
    useCompactCellMode: Boolean = true,
    onToggleCompactCellMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    var isStructuredFilterDialogOpen by remember { mutableStateOf(false) }
    var structuredFilterDraft by remember { mutableStateOf(StructuredFilterDraftState()) }

    val selectedStructuredOperator = structuredFilterDraft.selectedOperator()
    val canApplyStructuredFilter = structuredFilterDraft.canApply()

    val closeStructuredFilterDialog = {
        isStructuredFilterDialogOpen = false
        structuredFilterDraft = StructuredFilterDraftState()
    }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("filter_bar"),
        elevation = 2.dp,
        color = KLogViewerTheme.customColors.toolbarSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterBarGroup(label = "Sources", testTag = "toolbar_group_sources") {
                sourceActions(
                    onOpenFileClick = onOpenFileClick,
                    onSftpClick = onSftpClick,
                    onS3Click = onS3Click
                )
                workspaceAddActions(
                    onAddFileClick = onAddFileClick,
                    onAddDirectoryClick = onAddDirectoryClick,
                    onAddSftpClick = onAddSftpClick,
                    onAddS3Click = onAddS3Click
                )
            }

            filterBarGroup(label = "Stream", testTag = "toolbar_group_stream") {
                streamActions(
                    isReversed = isReversed,
                    onToggleSortOrder = onToggleSortOrder,
                    isAutoScrollEnabled = isAutoScrollEnabled,
                    onToggleAutoScroll = onToggleAutoScroll,
                    isConnected = isConnected,
                    onToggleConnection = onToggleConnection,
                    onRefresh = onRefresh
                )
            }

            filterBarGroup(label = "View", testTag = "toolbar_group_view") {
                viewActions(
                    onToggleSidebar = onToggleSidebar,
                    onSplitClick = onSplitClick
                )
            }

            filterBarGroup(label = "More", testTag = "toolbar_group_more") {
                overflowMenu(
                    onToggleTheme = onToggleTheme,
                    showAnsiColors = showAnsiColors,
                    onToggleAnsiColors = onToggleAnsiColors,
                    useCompactCellMode = useCompactCellMode,
                    onToggleCompactCellMode = onToggleCompactCellMode,
                    onEditPatternMapping = onEditPatternMapping
                )
            }

            filterBarGroup(
                label = "Filters",
                modifier = Modifier.weight(1f),
                testTag = "toolbar_group_filters"
            ) {
                TimeFilterControls(
                    preset = timeFilterPreset,
                    validationMessage = timeFilterValidationMessage,
                    onApplyPreset = onApplyTimeFilterPreset,
                    onClear = onClearTimeFilter
                )

                structuredFilterActions(
                    draft = structuredFilterDraft,
                    selectedOperator = selectedStructuredOperator,
                    canApply = canApplyStructuredFilter,
                    isDialogOpen = isStructuredFilterDialogOpen,
                    onOpen = { isStructuredFilterDialogOpen = true },
                    onDraftChange = { structuredFilterDraft = it },
                    onApply = {
                        onAddQuery(structuredFilterDraft.buildQuery())
                        closeStructuredFilterDialog()
                    },
                    onCancel = closeStructuredFilterDialog
                )

                filterQueryInputArea(
                    modifier = Modifier.weight(1f),
                    filterQueries = filterQueries,
                    text = textState,
                    onTextChange = { textState = it },
                    onAddQuery = onAddQuery,
                    onRemoveQuery = onRemoveQuery,
                    onClearQueries = {
                        onClearQueries()
                        textState = ""
                    }
                )
            }

            resultsCount(matchesCount = matchesCount, totalCount = totalCount)
        }
    }
}

@Composable
private fun RowScope.filterBarGroup(
    label: String,
    modifier: Modifier = Modifier,
    testTag: String,
    content: @Composable RowScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(
                color = MaterialTheme.colors.background.copy(alpha = 0.22f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.overline,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.72f),
            maxLines = 1
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            content = content
        )
    }
}

@Composable
private fun sourceActions(
    onOpenFileClick: () -> Unit,
    onSftpClick: () -> Unit,
    onS3Click: () -> Unit
) {
    filterBarIcon(
        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
        tooltip = "Open Log File",
        onClick = onOpenFileClick,
        testTag = "toolbar_open_file"
    )
    filterBarIcon(
        icon = Icons.Default.Cloud,
        tooltip = "Connect to SFTP",
        onClick = onSftpClick,
        testTag = "toolbar_connect_sftp"
    )
    filterBarIcon(
        icon = Icons.Default.CloudQueue,
        tooltip = "Connect to S3",
        onClick = onS3Click,
        testTag = "toolbar_connect_s3"
    )
}


@Composable
private fun streamActions(
    isReversed: Boolean,
    onToggleSortOrder: () -> Unit,
    isAutoScrollEnabled: Boolean,
    onToggleAutoScroll: () -> Unit,
    isConnected: Boolean,
    onToggleConnection: () -> Unit,
    onRefresh: () -> Unit
) {
    filterBarIcon(
        icon = if (isReversed) Icons.Default.SwapVert else Icons.AutoMirrored.Filled.Sort,
        tooltip = if (isReversed) "Newest First" else "Oldest First",
        onClick = onToggleSortOrder,
        testTag = "toggle_sort_order"
    )
    filterBarIcon(
        icon = Icons.Default.ArrowDownward,
        tooltip = if (isAutoScrollEnabled) "Auto-scroll ON" else "Auto-scroll OFF",
        onClick = onToggleAutoScroll,
        tint = if (isAutoScrollEnabled) MaterialTheme.colors.primary else LocalContentColor.current,
        testTag = if (isAutoScrollEnabled) "auto-scroll_on" else "auto-scroll_off"
    )
    filterBarIcon(
        icon = if (isConnected) Icons.Default.Link else Icons.Default.LinkOff,
        tooltip = if (isConnected) "Connected (Click to Disconnect)" else "Disconnected (Click to Connect)",
        onClick = onToggleConnection,
        tint = if (isConnected) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        },
        testTag = if (isConnected) "connected" else "disconnected"
    )
    filterBarIcon(
        icon = Icons.Default.Refresh,
        tooltip = "Refresh Sources",
        onClick = onRefresh,
        testTag = "toolbar_refresh"
    )
}

@Composable
private fun viewActions(
    onToggleSidebar: () -> Unit,
    onSplitClick: () -> Unit
) {
    filterBarIcon(
        icon = Icons.AutoMirrored.Filled.ViewSidebar,
        tooltip = "Toggle Sidebar",
        onClick = onToggleSidebar,
        testTag = "toggle_sidebar"
    )
    filterBarIcon(
        icon = Icons.Default.VerticalSplit,
        tooltip = "Split Horizontal",
        onClick = onSplitClick,
        testTag = "split_horizontal"
    )
}

@Composable
private fun overflowMenu(
    onToggleTheme: () -> Unit,
    showAnsiColors: Boolean,
    onToggleAnsiColors: () -> Unit,
    useCompactCellMode: Boolean = true,
    onToggleCompactCellMode: () -> Unit = {},
    onEditPatternMapping: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        filterBarIcon(
            icon = Icons.Default.MoreVert,
            tooltip = "More display options",
            onClick = { menuExpanded = true },
            testTag = "toolbar_more_menu"
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            compactMenuItem(
                text = "Toggle Theme",
                onClick = {
                    menuExpanded = false
                    onToggleTheme()
                },
                modifier = Modifier.testTag("toolbar_toggle_theme")
            )
            compactMenuItem(
                text = if (showAnsiColors) "ANSI Colors: ON" else "ANSI Colors: OFF",
                onClick = {
                    menuExpanded = false
                    onToggleAnsiColors()
                },
                modifier = Modifier.testTag("toolbar_toggle_ansi")
            )
            compactMenuItem(
                text = if (useCompactCellMode) "Cell View: Compact" else "Cell View: Full",
                onClick = {
                    menuExpanded = false
                    onToggleCompactCellMode()
                },
                modifier = Modifier.testTag("toolbar_toggle_compact_mode")
            )
            if (onEditPatternMapping != null) {
                Divider()
                compactMenuItem(
                    text = "Edit Pattern Mapping",
                    onClick = {
                        menuExpanded = false
                        onEditPatternMapping()
                    },
                    modifier = Modifier.testTag("toolbar_menu_edit_pattern_mapping")
                )
            }
        }
    }
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
            filterBarIcon(
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
                        compactMenuItem(
                            text = presetOption.displayLabel(),
                            onClick = {
                                presetMenuExpanded = false
                                onApplyPreset(presetOption)
                            },
                            modifier = Modifier.testTag("time_filter_preset_${presetOption.name.lowercase()}")
                        )
                    }

                Divider()

                compactMenuItem(
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
internal fun filterBarIcon(
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
internal fun filterChip(
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
            Text(text = query, style = MaterialTheme.typography.body2)
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
