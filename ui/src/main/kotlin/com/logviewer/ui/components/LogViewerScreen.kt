package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import com.logviewer.ui.mvi.LogViewerIntent
import com.logviewer.ui.theme.LogViewerTheme
import com.logviewer.ui.viewmodel.LogViewerViewModel
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun LogViewerScreen(viewModel: LogViewerViewModel) {
    val state by viewModel.state.collectAsState()
    val activeTab = state.activeTab
    val scaffoldState = rememberScaffoldState()

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.logviewer.ui.mvi.LogViewerEvent.ShowError -> {
                    scaffoldState.snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val pendingDialog = state.pendingDialog
    if (pendingDialog != null) {
        FileDialog(
            onCloseRequest = { results ->
                viewModel.handleIntent(LogViewerIntent.DismissDialog)
                if (results != null) {
                    when (pendingDialog) {
                        com.logviewer.ui.mvi.LogViewerState.DialogType.OPEN -> viewModel.handleIntent(LogViewerIntent.LoadFiles(results))
                        com.logviewer.ui.mvi.LogViewerState.DialogType.ADD -> viewModel.handleIntent(LogViewerIntent.AddToWorkspace(results))
                    }
                }
            }
        )
    }

    LogViewerTheme(darkTheme = state.isDarkMode) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                Column {
                    LogTabRow(
                        tabs = state.tabs,
                        activeTabId = state.activeTabId,
                        onTabClick = { viewModel.handleIntent(LogViewerIntent.SwitchTab(it)) },
                        onCloseClick = { viewModel.handleIntent(LogViewerIntent.CloseTab(it)) },
                        onAddClick = { viewModel.handleIntent(LogViewerIntent.AddTab) }
                    )
                    RibbonBar(
                        onOpenClick = { viewModel.handleIntent(LogViewerIntent.ShowOpenDialog) },
                        onAddClick = { viewModel.handleIntent(LogViewerIntent.ShowAddDialog) },
                        onClearClick = { viewModel.handleIntent(LogViewerIntent.ClearLogs) },
                        onToggleTheme = { viewModel.handleIntent(LogViewerIntent.ToggleTheme) },
                        onToggleSidebar = { viewModel.handleIntent(LogViewerIntent.ToggleSidebar) },
                        searchQuery = activeTab?.searchQuery ?: "",
                        onSearchQueryChange = { viewModel.handleIntent(LogViewerIntent.UpdateSearch(it)) },
                        matchesCount = activeTab?.filteredLogs?.size ?: 0,
                        totalCount = activeTab?.logs?.size ?: 0,
                        levelFilters = activeTab?.levelFilters ?: emptySet(),
                        onToggleLevel = { viewModel.handleIntent(LogViewerIntent.ToggleLevel(it)) }
                    )
                }
            },
            bottomBar = {
                StatusBar(
                    filePath = activeTab?.filePath ?: "",
                    lineCount = activeTab?.logs?.size ?: 0
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
                    onToggleExpanded = { viewModel.handleIntent(LogViewerIntent.ToggleSidebar) },
                    isDarkMode = state.isDarkMode,
                    onToggleTheme = { viewModel.handleIntent(LogViewerIntent.ToggleTheme) },
                    levelFilters = activeTab?.levelFilters ?: emptySet(),
                    onToggleLevel = { level -> viewModel.handleIntent(LogViewerIntent.ToggleLevel(level)) }
                )

                Column(modifier = Modifier.weight(1f)) {
                    if (activeTab?.error != null) {
                        Text(
                            text = "Error: ${activeTab.error}",
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            if (activeTab?.isLoading == true) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else {
                                LogList(
                                    logs = activeTab?.filteredLogs ?: emptyList(),
                                    searchQuery = activeTab?.searchQuery ?: "",
                                    isDarkMode = state.isDarkMode,
                                    selectedEntry = activeTab?.selectedEntry,
                                    onEntryClick = { viewModel.handleIntent(LogViewerIntent.SelectEntry(it)) }
                                )
                            }
                        }
                        
                        // Detail Pane (Split View)
                        if (activeTab?.selectedEntry != null) {
                            Divider(modifier = Modifier.height(1.dp).fillMaxWidth())
                            Box(modifier = Modifier.height(250.dp)) {
                                LogEntryDetails(
                                    entry = activeTab.selectedEntry,
                                    onClose = { viewModel.handleIntent(LogViewerIntent.SelectEntry(null)) }
                                )
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
    tabs: List<com.logviewer.ui.mvi.TabState>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onCloseClick: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Surface(elevation = 4.dp, color = MaterialTheme.colors.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0),
                modifier = Modifier.weight(1f),
                edgePadding = 0.dp,
                backgroundColor = MaterialTheme.colors.primary,
                contentColor = MaterialTheme.colors.onPrimary
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = tab.id == activeTabId,
                        onClick = { onTabClick(tab.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.button,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onCloseClick(tab.id) },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = if (tab.id == activeTabId) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onPrimary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add tab", tint = MaterialTheme.colors.onPrimary)
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
