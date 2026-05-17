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
import androidx.compose.ui.graphics.Color
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

@Composable
fun KLogViewerScreen(viewModel: KLogViewerViewModel) {
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
    if (pendingDialog != null && pendingDialog != com.klogviewer.ui.mvi.KLogViewerState.DialogType.RECENT_ITEMS) {
        FileDialog(
            onCloseRequest = { results ->
                viewModel.handleIntent(KLogViewerIntent.DismissDialog)
                if (results != null) {
                    when (pendingDialog) {
                        com.klogviewer.ui.mvi.KLogViewerState.DialogType.OPEN -> viewModel.handleIntent(KLogViewerIntent.LoadFiles(results))
                        com.klogviewer.ui.mvi.KLogViewerState.DialogType.ADD -> viewModel.handleIntent(KLogViewerIntent.AddToWorkspace(results))
                    }
                }
            }
        )
    }

    if (pendingDialog == com.klogviewer.ui.mvi.KLogViewerState.DialogType.RECENT_ITEMS) {
        RecentItemsDialog(
            recentFiles = state.recentFiles,
            recentDirectories = state.recentDirectories,
            onSelect = { 
                viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(it)))
                viewModel.handleIntent(KLogViewerIntent.DismissDialog)
            },
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
                        onSplitClick = { viewModel.handleIntent(KLogViewerIntent.SplitHorizontal) },
                        matchesCount = activeWindow?.filteredLogs?.size ?: 0,
                        totalCount = activeWindow?.logs?.size ?: 0
                    )
                }
            },
            bottomBar = {
                StatusBar(
                    filePath = activeWindow?.filePath ?: "",
                    lineCount = activeWindow?.logs?.size ?: 0
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
                    levelCounts = activeWindow?.levelCounts ?: emptyMap()
                )

                Column(modifier = Modifier.weight(1f)) {
                    activeTab?.windows?.forEachIndexed { index, window ->
                        if (index > 0) {
                            Divider(modifier = Modifier.height(2.dp).fillMaxWidth(), color = MaterialTheme.colors.primary.copy(alpha = 0.5f))
                        }
                        
                        val isWindowActive = window.id == activeTab.activeWindowId
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (isWindowActive) MaterialTheme.colors.onSurface.copy(alpha = 0.02f) else Color.Transparent)
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
                                        Text(
                                            text = window.filePath,
                                            style = MaterialTheme.typography.caption.copy(
                                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
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
                                        selectedEntry = window.selectedEntry,
                                        onEntryClick = { 
                                            viewModel.handleIntent(KLogViewerIntent.SwitchWindow(window.id))
                                            viewModel.handleIntent(KLogViewerIntent.SelectEntry(it))
                                        },
                                        onColumnResize = { column, width ->
                                            viewModel.handleIntent(KLogViewerIntent.UpdateColumnWidth(column, width))
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
                                        isDarkMode = state.isDarkMode
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
    Surface(elevation = 4.dp, color = tabBackground) {
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
                                    style = MaterialTheme.typography.button.copy(fontSize = 12.sp),
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
                IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) {
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recently Opened Items") },
        text = {
            Column(modifier = Modifier.width(600.dp).heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (recentFiles.isNotEmpty()) {
                    Text("Files", style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(vertical = 8.dp))
                    recentFiles.forEach { path ->
                        TextButton(
                            onClick = { onSelect(path) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = path,
                                style = MaterialTheme.typography.body2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                if (recentFiles.isNotEmpty() && recentDirectories.isNotEmpty()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }

                if (recentDirectories.isNotEmpty()) {
                    Text("Directories", style = MaterialTheme.typography.subtitle2, modifier = Modifier.padding(vertical = 8.dp))
                    recentDirectories.forEach { path ->
                        TextButton(
                            onClick = { onSelect(path) },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = path,
                                style = MaterialTheme.typography.body2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
