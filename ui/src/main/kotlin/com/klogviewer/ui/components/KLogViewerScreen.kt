package com.klogviewer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.mvi.LogWindow
import com.klogviewer.ui.mvi.TabState
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.ui.theme.KLogViewerTheme
import com.klogviewer.ui.viewmodel.KLogViewerViewModel

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

    DialogHandler(state, viewModel, dialogProvider)

    KLogViewerTheme(darkTheme = state.isDarkMode) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                LogTopBar(state, activeWindow, viewModel)
            },
            bottomBar = {
                LogBottomBar(activeWindow, viewModel)
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

                LogWindowList(
                    activeTab = activeTab,
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DialogHandler(
    state: KLogViewerState,
    viewModel: KLogViewerViewModel,
    dialogProvider: DialogProvider
) {
    val pendingDialog = state.pendingDialog
    LaunchedEffect(pendingDialog) {
        if (pendingDialog == KLogViewerState.DialogType.OPEN ||
            pendingDialog == KLogViewerState.DialogType.OPEN_DIRECTORY ||
            pendingDialog == KLogViewerState.DialogType.ADD ||
            pendingDialog == KLogViewerState.DialogType.ADD_DIRECTORY
        ) {
            val title = when (pendingDialog) {
                KLogViewerState.DialogType.OPEN -> "Select Log File"
                KLogViewerState.DialogType.OPEN_DIRECTORY -> "Select Log Directory"
                KLogViewerState.DialogType.ADD -> "Add Log File"
                KLogViewerState.DialogType.ADD_DIRECTORY -> "Add Log Directory"
                else -> ""
            }
            val path = if (pendingDialog == KLogViewerState.DialogType.OPEN_DIRECTORY ||
                pendingDialog == KLogViewerState.DialogType.ADD_DIRECTORY
            ) {
                dialogProvider.showOpenDirectoryDialog(title)
            } else {
                dialogProvider.showOpenFileDialog(title)
            }

            viewModel.handleIntent(KLogViewerIntent.DismissDialog)

            if (path != null) {
                val paths = listOf(path)
                when (pendingDialog) {
                    KLogViewerState.DialogType.OPEN,
                    KLogViewerState.DialogType.OPEN_DIRECTORY -> viewModel.handleIntent(
                        KLogViewerIntent.LoadFiles(paths)
                    )

                    KLogViewerState.DialogType.ADD,
                    KLogViewerState.DialogType.ADD_DIRECTORY -> viewModel.handleIntent(
                        KLogViewerIntent.AddToWorkspace(
                            paths
                        )
                    )
                    else -> {}
                }
            }
        }
    }

    if (pendingDialog == KLogViewerState.DialogType.RECENT_ITEMS) {
        RecentItemsDialog(
            recentFiles = state.recentFiles,
            recentDirectories = state.recentDirectories,
            localFileSystem = viewModel.localFileSystem,
            onSelect = { path ->
                viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path)))
                viewModel.handleIntent(KLogViewerIntent.DismissDialog)
            },
            onRemoveItem = { viewModel.handleIntent(KLogViewerIntent.RemoveRecentItem(it)) },
            onClearMissing = { viewModel.handleIntent(KLogViewerIntent.ClearMissingRecentItems) },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }
        )
    }

    if (state.pendingDialog == KLogViewerState.DialogType.SFTP_CONNECT ||
        state.pendingDialog == KLogViewerState.DialogType.SFTP_ADD
    ) {
        val isAdd = state.pendingDialog == KLogViewerState.DialogType.SFTP_ADD
        SftpConnectionDialog(
            savedConnections = state.sftpConnections,
            onConnect = { name, host, port, user, auth, path ->
                viewModel.handleIntent(
                    KLogViewerIntent.ConnectSftp(
                        name,
                        host,
                        port,
                        user,
                        auth,
                        path,
                        addToWorkspace = isAdd
                    )
                )
            },
            onSave = { config ->
                viewModel.handleIntent(KLogViewerIntent.SaveSftpConnection(config))
            },
            onDelete = { name ->
                viewModel.handleIntent(KLogViewerIntent.DeleteSftpConnection(name))
            },
            onBrowse = { host, port, user, auth, path ->
                val config = SftpConfig(
                    "Temporary",
                    com.klogviewer.domain.model.Host(host),
                    com.klogviewer.domain.model.Port(port),
                    com.klogviewer.domain.model.Username(user),
                    auth
                )
                viewModel.handleIntent(KLogViewerIntent.BrowseSftp(config, path))
            },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) },
            dialogProvider = dialogProvider
        )
    }

    if (pendingDialog == KLogViewerState.DialogType.SFTP_BROWSE) {
        RemoteFileBrowserDialog(
            files = state.remoteFiles,
            currentPath = state.remoteBrowsePath,
            isLoading = state.isRemoteLoading,
            onNavigate = { path -> viewModel.handleIntent(KLogViewerIntent.NavigateRemote(path)) },
            onSelectFiles = { paths ->
                val config = state.currentSftpConfig
                if (config != null) {
                    viewModel.handleIntent(
                        KLogViewerIntent.ConnectMultipleSftp(
                            config,
                            paths,
                            addToWorkspace = state.isAddMode
                        )
                    )
                }
            },
            onSelectDirectory = { path ->
                val config = state.currentSftpConfig
                if (config != null) {
                    viewModel.handleIntent(
                        KLogViewerIntent.ConnectSftpDirectory(
                            config,
                            path,
                            addToWorkspace = state.isAddMode
                        )
                    )
                }
            },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }
        )
    }
}

@Composable
private fun LogTopBar(
    state: KLogViewerState,
    activeWindow: LogWindow?,
    viewModel: KLogViewerViewModel
) {
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
            onAddDirectoryClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDirectoryDialog) },
            onAddSftpClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddSftpDialog) },
            onToggleTheme = { viewModel.handleIntent(KLogViewerIntent.ToggleTheme) },
            onToggleSidebar = { viewModel.handleIntent(KLogViewerIntent.ToggleSidebar) },
            isReversed = activeWindow?.isReversed ?: false,
            onToggleSortOrder = { viewModel.handleIntent(KLogViewerIntent.ToggleSortOrder) },
            isAutoScrollEnabled = activeWindow?.isAutoScrollEnabled ?: true,
            onToggleAutoScroll = { viewModel.handleIntent(KLogViewerIntent.ToggleAutoScroll) },
            showAnsiColors = activeWindow?.showAnsiColors ?: true,
            onToggleAnsiColors = { viewModel.handleIntent(KLogViewerIntent.ToggleAnsiColors) },
            isConnected = activeWindow?.isConnected ?: true,
            onToggleConnection = { viewModel.handleIntent(KLogViewerIntent.ToggleConnection) },
            onSplitClick = { viewModel.handleIntent(KLogViewerIntent.SplitHorizontal) },
            matchesCount = activeWindow?.filteredLogs?.size ?: 0,
            totalCount = activeWindow?.logs?.size ?: 0
        )
    }
}

@Composable
private fun LogBottomBar(
    activeWindow: LogWindow?,
    viewModel: KLogViewerViewModel
) {
    val availableParsers = remember {
        listOf("Simple", "JSON", "logfmt") + viewModel.heuristicProbe.registry.getAllTemplates().map { it.name }
    }
    StatusBar(
        filePath = activeWindow?.filePath ?: "",
        lineCount = activeWindow?.logs?.size ?: 0,
        parserName = activeWindow?.parserName,
        availableParsers = availableParsers,
        onParserSelect = { name ->
            activeWindow?.id?.let { id ->
                viewModel.handleIntent(KLogViewerIntent.ChangeParser(id, name))
            }
        },
        isMissing = activeWindow?.let { it.missingSourceIds.contains(it.filePath) || it.error != null } ?: false,
        isConnected = activeWindow?.isConnected ?: true
    )
}

@Composable
private fun LogWindowList(
    activeTab: TabState?,
    state: KLogViewerState,
    viewModel: KLogViewerViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        activeTab?.windows?.forEachIndexed { index, window ->
            if (index > 0) {
                Divider(
                    modifier = Modifier.height(2.dp).fillMaxWidth(),
                    color = MaterialTheme.colors.primary.copy(alpha = 0.5f)
                )
            }

            LogWindowItem(
                window = window,
                isWindowActive = window.id == activeTab.activeWindowId,
                showSplitIndicator = activeTab.windows.size > 1,
                state = state,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LogWindowItem(
    window: LogWindow,
    isWindowActive: Boolean,
    showSplitIndicator: Boolean,
    state: KLogViewerState,
    viewModel: KLogViewerViewModel,
    modifier: Modifier = Modifier
) {
    val activeBorderColor = MaterialTheme.colors.primary.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .testTag("window_${window.id}")
            .background(if (isWindowActive) MaterialTheme.colors.onSurface.copy(alpha = 0.02f) else Color.Transparent)
            .drawBehind {
                if (isWindowActive && showSplitIndicator) {
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
        if (window.filePath.isNotEmpty() || showSplitIndicator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (window.isConnected) Color.Transparent else MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (window.filePath.isNotEmpty()) {
                    val isPrimaryPathMissing = window.missingSourceIds.contains(window.filePath)
                    val isAnySourceMissing = if (window.isDirectory) isPrimaryPathMissing else window.missingSourceIds.isNotEmpty()
                    val isWindowError = window.error != null
                    Text(
                        text = window.filePath,
                        style = MaterialTheme.typography.caption.copy(
                            color = if (isPrimaryPathMissing || isWindowError) Color.Red else if (isAnySourceMissing) Color(0xFFFFA500) else MaterialTheme.colors.onSurface.copy(
                                alpha = 0.5f
                            ),
                            textDecoration = if (isPrimaryPathMissing) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWindowActive && showSplitIndicator) {
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

                    if (showSplitIndicator) {
                        TooltipWrapper(tooltip = "Close split") {
                            IconButton(
                                onClick = {
                                    viewModel.handleIntent(
                                        KLogViewerIntent.CloseWindow(
                                            window.id
                                        )
                                    )
                                },
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
                    missingSourceIds = window.missingSourceIds,
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
                            viewModel.handleIntent(
                                KLogViewerIntent.ToggleEntrySelection(
                                    index,
                                    isShift,
                                    isMeta
                                )
                            )
                        } else {
                            viewModel.handleIntent(KLogViewerIntent.SwitchWindow(window.id))
                        }
                    },
                    onColumnResize = { column, width ->
                        viewModel.handleIntent(
                            KLogViewerIntent.UpdateColumnWidth(
                                window.id,
                                column,
                                width
                            )
                        )
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
                        val isAnyPrimaryWindowMissing = tab.windows.any { it.missingSourceIds.contains(it.filePath) }
                        val isAnySecondarySourceMissing = tab.windows.any { window ->
                            if (window.isDirectory) false
                            else (window.missingSourceIds - window.filePath).isNotEmpty()
                        }
                        val isAnyWindowError = tab.windows.any { it.error != null }
                        Tab(
                            selected = tab.id == activeTabId,
                            onClick = { onTabClick(tab.id) },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayTitle = if (tab.activeWindow?.isDirectory == true) {
                                    "${tab.title} [${tab.activeWindow!!.sourceIds.size - 1}]"
                                } else {
                                    tab.title
                                }
                                val tooltip = tab.windows.map { it.filePath }.distinct().filter { it.isNotEmpty() }.joinToString("\n").ifEmpty { tab.title }
                                TooltipWrapper(tooltip = tooltip) {
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.button.copy(
                                            fontSize = 12.sp,
                                            color = if (isAnyPrimaryWindowMissing || isAnyWindowError) Color.Red else if (isAnySecondarySourceMissing) Color(0xFFFFA500) else Color.Unspecified,
                                            textDecoration = if (isAnyPrimaryWindowMissing) TextDecoration.LineThrough else TextDecoration.None
                                        ),
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TooltipWrapper(tooltip = "Close tab") {
                                    IconButton(
                                        onClick = { onCloseClick(tab.id) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tab",
                                            tint = if (tab.id == activeTabId) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(
                                                alpha = 0.6f
                                            ),
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
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add tab",
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
