package com.klogviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.awt.AwtWindow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.theme.KLogViewerTheme
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun KLogViewerScreen(
    viewModel: KLogViewerViewModel,
    dialogProvider: DialogProvider = AwtDialogProvider()
) {
    val state by viewModel.state.collectAsState()
    val activeTab = state.activeTab
    val activeWindow = activeTab?.activeWindow
    val scaffoldState = rememberScaffoldState()

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.klogviewer.ui.mvi.KLogViewerEvent.ShowError -> {
                    scaffoldState.snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val pendingDialog = state.pendingDialog
    LaunchedEffect(pendingDialog) {
        if (pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN ||
            pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN_DIRECTORY ||
            pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.ADD
        ) {
            val title = when (pendingDialog) {
                com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN -> "Select Log File"
                com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN_DIRECTORY -> "Select Log Directory"
                com.klogviewer.ui.mvi.KLogViewerState.DialogType.ADD -> "Add Log File"
                com.klogviewer.ui.mvi.KLogViewerState.DialogType.RECENT_ITEMS -> ""
                com.klogviewer.ui.mvi.KLogViewerState.DialogType.MISSING_FILE -> ""
            }
            val file = if (pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN_DIRECTORY) {
                dialogProvider.showOpenDirectoryDialog(title)
            } else {
                dialogProvider.showOpenFileDialog(title)
            }
            
            viewModel.handleIntent(KLogViewerIntent.DismissDialog)
            
            if (file != null) {
                val paths = listOf(file.absolutePath)
                when (pendingDialog) {
                    com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN,
                    com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN_DIRECTORY -> viewModel.handleIntent(KLogViewerIntent.LoadFiles(paths))
                    com.klogviewer.ui.mvi.KLogViewerState.DialogType.ADD -> viewModel.handleIntent(KLogViewerIntent.AddToWorkspace(paths))
                    com.klogviewer.ui.mvi.KLogViewerState.DialogType.RECENT_ITEMS,
                    com.klogviewer.ui.mvi.KLogViewerState.DialogType.MISSING_FILE -> {}
                }
            }
        }
    }

    if (state.pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.MISSING_FILE && state.missingPath != null) {
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) },
            title = { Text("File Not Found") },
            text = { Text("The file '${state.missingPath}' no longer exists. Would you like to remove it from the recent items list?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.handleIntent(KLogViewerIntent.RemoveRecentItem(state.missingPath!!))
                    viewModel.handleIntent(KLogViewerIntent.DismissDialog)
                }) {
                    Text("Remove from List")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }) {
                    Text("Keep in List")
                }
            }
        )
    }

    if (pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.RECENT_ITEMS) {
        RecentItemsDialog(
            recentFiles = state.recentFiles,
            recentDirectories = state.recentDirectories,
            onSelect = { path ->
                viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path)))
                if (java.io.File(path).exists()) {
                    viewModel.handleIntent(KLogViewerIntent.DismissDialog)
                }
            },
            onRemoveItem = { viewModel.handleIntent(KLogViewerIntent.RemoveRecentItem(it)) },
            onClearMissing = { viewModel.handleIntent(KLogViewerIntent.ClearMissingRecentItems) },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }
        )
    }

    KLogViewerTheme(darkTheme = state.isDarkMode) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                Column {
                    LogTabRow(
                        tabs = state.tabs,
                        activeTabId = state.activeTabId,
                        onTabClick = { viewModel.handleIntent(KLogViewerIntent.SwitchTab(it)) },
                        onCloseClick = { viewModel.handleIntent(KLogViewerIntent.CloseTab(it)) },
                        onAddClick = { viewModel.handleIntent(KLogViewerIntent.AddTab) }
                    )
                    FilterBar(
                        filterQueries = activeWindow?.filterQueries ?: emptyList(),
                        onAddQuery = { viewModel.handleIntent(KLogViewerIntent.AddFilterQuery(it)) },
                        onRemoveQuery = { viewModel.handleIntent(KLogViewerIntent.RemoveFilterQuery(it)) },
                        onClearQueries = { viewModel.handleIntent(KLogViewerIntent.ClearFilterQueries) },
                        onAddClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDialog) },
                        onToggleTheme = { viewModel.handleIntent(KLogViewerIntent.ToggleTheme) },
                        onToggleSidebar = { viewModel.handleIntent(KLogViewerIntent.ToggleSidebar) },
                        isReversed = activeWindow?.isReversed ?: false,
                        onToggleSortOrder = { viewModel.handleIntent(KLogViewerIntent.ToggleSortOrder) },
                        isAutoScrollEnabled = activeWindow?.isAutoScrollEnabled ?: true,
                        onToggleAutoScroll = { viewModel.handleIntent(KLogViewerIntent.ToggleAutoScroll) },
                        showAnsiColors = activeWindow?.showAnsiColors ?: true,
                        onToggleAnsiColors = { viewModel.handleIntent(KLogViewerIntent.ToggleAnsiColors) },
                        onSplitClick = { viewModel.handleIntent(KLogViewerIntent.SplitHorizontal) },
                        matchesCount = activeWindow?.filteredLogs?.size ?: 0,
                        totalCount = activeWindow?.logs?.size ?: 0
                    )
                }
            },
            bottomBar = {
                StatusBar(
                    filePath = activeWindow?.filePath ?: "",
                    lineCount = activeWindow?.logs?.size ?: 0,
                    isMissing = activeWindow?.missingSourceIds?.isNotEmpty() ?: false
                )
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Sidebar(
                    isExpanded = state.isSidebarExpanded,
                    levelFilters = activeWindow?.levelFilters ?: emptySet(),
                    onToggleLevel = { level -> viewModel.handleIntent(KLogViewerIntent.ToggleLevel(level)) },
                    onToggleAllLevels = { viewModel.handleIntent(KLogViewerIntent.ToggleAllLevels) },
                    levelCounts = activeWindow?.levelCounts ?: emptyMap()
                )

                Column(modifier = Modifier.weight(1f)) {
                    activeTab?.windows?.forEachIndexed { index, window ->
                        if (index > 0) {
                            Divider(modifier = Modifier.height(2.dp).fillMaxWidth(), color = MaterialTheme.colors.primary.copy(alpha = 0.5f))
                        }
                        
                        val isWindowActive = window.id == activeTab.activeWindowId
                        val activeBorderColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("window_${window.id}")
                                .background(if (isWindowActive) MaterialTheme.colors.onSurface.copy(alpha = 0.02f) else Color.Transparent)
                                .drawBehind {
                                    if (isWindowActive && activeTab.windows.size > 1) {
                                        drawLine(
                                            color = activeBorderColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                }
                                .padding(top = 4.dp)
                        ) {
                            // Window Header (File path, Active badge, Close button)
                            if (window.filePath.isNotEmpty() || activeTab.windows.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (window.filePath.isNotEmpty()) {
                                        val isAnySourceMissing = window.missingSourceIds.isNotEmpty()
                                        Text(
                                            text = window.filePath,
                                            style = MaterialTheme.typography.caption.copy(
                                                color = if (isAnySourceMissing) Color.Red else MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                                                textDecoration = if (isAnySourceMissing) TextDecoration.LineThrough else TextDecoration.None
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isWindowActive && activeTab.windows.size > 1) {
                                            Surface(
                                                color = MaterialTheme.colors.primary.copy(alpha = 0.1f),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Active",
                                                    style = MaterialTheme.typography.caption,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        
                                        if (activeTab.windows.size > 1) {
                                            TooltipWrapper(tooltip = "Close split") {
                                                IconButton(
                                                    onClick = { viewModel.handleIntent(KLogViewerIntent.CloseWindow(window.id)) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Close Split",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (window.error != null) {
                                Text(
                                    text = "Error: ${window.error}",
                                    color = MaterialTheme.colors.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        viewModel.handleIntent(KLogViewerIntent.SwitchWindow(window.id))
                                    }
                            ) {
                                if (window.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                } else {
                                    LogList(
                                        logs = window.filteredLogs,
                                        filterQueries = window.filterQueries,
                                        isDarkMode = state.isDarkMode,
                                        sourceIds = window.sourceIds,
                                        columns = window.columns,
                                        columnWidths = window.columnWidths,
                                        isAutoScrollEnabled = window.isAutoScrollEnabled,
                                        showAnsiColors = window.showAnsiColors,
                                        selectedIndices = window.selectedIndices,
                                        onEntryClick = { 
                                            if (isWindowActive) {
                                                viewModel.handleIntent(KLogViewerIntent.SelectEntry(it))
                                            } else {
                                                viewModel.handleIntent(KLogViewerIntent.SwitchWindow(window.id))
                                            }
                                        },
                                        onToggleSelection = { index, isShift, isMeta ->
                                            if (isWindowActive) {
                                                viewModel.handleIntent(KLogViewerIntent.ToggleEntrySelection(index, isShift, isMeta))
                                            } else {
                                                viewModel.handleIntent(KLogViewerIntent.SwitchWindow(window.id))
                                            }
                                        },
                                        onColumnResize = { column, width ->
                                            viewModel.handleIntent(KLogViewerIntent.UpdateColumnWidth(window.id, column, width))
                                        }
                                    )
                                }
                            }
                            
                            // Detail Pane (Split View within Window)
                            if (window.selectedEntry != null) {
                                Divider(modifier = Modifier.height(1.dp).fillMaxWidth())
                                Box(modifier = Modifier.height(200.dp)) {
                                    LogEntryDetails(
                                        entry = window.selectedEntry,
                                        onClose = { viewModel.handleIntent(KLogViewerIntent.SelectEntry(null)) },
                                        filterQueries = window.filterQueries,
                                        isDarkMode = state.isDarkMode,
                                        showAnsiColors = window.showAnsiColors
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogTabRow(
    tabs: List<com.klogviewer.ui.mvi.TabState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onCloseClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    val tabBackground = KLogViewerTheme.customColors.tabBackground
    Surface(elevation = 4.dp, color = tabBackground, modifier = Modifier.testTag("tab_row")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (tabs.isNotEmpty()) {
                val selectedTabIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceIn(0, tabs.size - 1)
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.weight(1f).height(32.dp),
                    edgePadding = 0.dp,
                    backgroundColor = tabBackground,
                    contentColor = MaterialTheme.colors.onSurface,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.Indicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                            )
                        }
                    }
                ) {
                    tabs.forEach { tab ->
                        val isAnyWindowMissing = tab.windows.any { it.missingSourceIds.isNotEmpty() }
                        Tab(
                            selected = tab.id == activeTabId,
                            onClick = { onTabClick(tab.id) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.button.copy(
                                        fontSize = 12.sp,
                                        color = if (isAnyWindowMissing) Color.Red else Color.Unspecified,
                                        textDecoration = if (isAnyWindowMissing) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TooltipWrapper(tooltip = "Close tab") {
                                    IconButton(
                                        onClick = { onCloseClick(tab.id) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = if (tab.id == activeTabId) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            TooltipWrapper(tooltip = "Add new tab") {
                IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp).testTag("add_tab_button")) {
                    Icon(Icons.Default.Add, contentDescription = "Add tab", tint = MaterialTheme.colors.onSurface, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun FileDialog(
    onCloseRequest: (result: List<String>?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(null as Frame?, "Select Log File", LOAD) {
            init {
                isMultipleMode = true
            }
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    val selectedFiles = files
                    if (selectedFiles != null && selectedFiles.isNotEmpty()) {
                        onCloseRequest(selectedFiles.map { it.absolutePath })
                    } else {
                        onCloseRequest(null)
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
fun RecentItemsDialog(
    recentFiles: List<String>,
    recentDirectories: List<String>,
    onSelect: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearMissing: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasMissing = recentFiles.any { !File(it).exists() } || recentDirectories.any { !File(it).exists() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recently Opened Items") },
        text = {
            Column(modifier = Modifier.width(600.dp).heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (hasMissing) {
                    Surface(
                        color = MaterialTheme.colors.error.copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Some items no longer exist on disk.",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.error
                            )
                            TextButton(onClick = onClearMissing) {
                                Text("Clear Missing", style = MaterialTheme.typography.caption, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (recentFiles.isNotEmpty()) {
                    Text("Files", style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(vertical = 8.dp))
                    recentFiles.forEach { path ->
                        RecentItemRow(path, onSelect, onRemoveItem, isMissing = !File(path).exists())
                    }
                }
                
                if (recentFiles.isNotEmpty() && recentDirectories.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (recentDirectories.isNotEmpty()) {
                    Text("Directories", style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(vertical = 8.dp))
                    recentDirectories.forEach { path ->
                        RecentItemRow(path, onSelect, onRemoveItem, isMissing = !File(path).exists())
                    }
                }
                
                if (recentFiles.isEmpty() && recentDirectories.isEmpty()) {
                    Text("No recent items found.", modifier = Modifier.padding(16.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RecentItemRow(
    path: String,
    onSelect: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    isMissing: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { onSelect(path) },
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = path,
                style = MaterialTheme.typography.body2.copy(
                    textDecoration = if (isMissing) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None,
                    color = if (isMissing) MaterialTheme.colors.onSurface.copy(alpha = 0.4f) else MaterialTheme.colors.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(onClick = { onRemoveItem(path) }, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove from history",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
