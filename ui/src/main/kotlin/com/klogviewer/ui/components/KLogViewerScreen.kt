package com.klogviewer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.ui.mvi.*
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
                    dialogProvider.showMessageDialog("Error", event.message)
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

    state.pendingPlaintextSecretSave?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(KLogViewerIntent.DeclinePlaintextSecretSave) },
            title = { Text(prompt.title) },
            text = { Text(prompt.message) },
            confirmButton = {
                TextButton(onClick = { viewModel.handleIntent(KLogViewerIntent.ConfirmPlaintextSecretSave) }) {
                    Text("Save in plaintext")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleIntent(KLogViewerIntent.DeclinePlaintextSecretSave) }) {
                    Text("Cancel")
                }
            }
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

    if (state.pendingDialog == KLogViewerState.DialogType.S3_CONNECT ||
        state.pendingDialog == KLogViewerState.DialogType.S3_ADD
    ) {
        val isAdd = state.pendingDialog == KLogViewerState.DialogType.S3_ADD
        S3ConnectionDialog(
            savedConnections = state.s3Connections,
            onConnect = { config ->
                viewModel.handleIntent(KLogViewerIntent.ConnectS3(config, addToWorkspace = isAdd))
            },
            onSave = { config ->
                viewModel.handleIntent(KLogViewerIntent.SaveS3Connection(config))
            },
            onDelete = { name ->
                viewModel.handleIntent(KLogViewerIntent.DeleteS3Connection(name))
            },
            onBrowse = { config ->
                viewModel.handleIntent(KLogViewerIntent.BrowseS3(config, config.prefix))
            },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }
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

    if (pendingDialog == KLogViewerState.DialogType.S3_BROWSE) {
        RemoteFileBrowserDialog(
            files = state.remoteFiles,
            currentPath = state.remoteBrowsePath,
            isLoading = state.isRemoteLoading,
            onNavigate = { path -> viewModel.handleIntent(KLogViewerIntent.BrowseS3(state.currentS3Config!!, path)) },
            onSelectFiles = { paths ->
                val config = state.currentS3Config
                if (config != null) {
                    viewModel.handleIntent(
                        KLogViewerIntent.ConnectMultipleS3(
                            config,
                            paths,
                            addToWorkspace = state.isAddMode
                        )
                    )
                }
            },
            onSelectDirectory = { path ->
                val config = state.currentS3Config
                if (config != null) {
                    viewModel.handleIntent(
                        KLogViewerIntent.ConnectS3Directory(
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
            onOpenFileClick = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDialog) },
            onOpenDirectoryClick = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDirectoryDialog) },
            onSftpClick = { viewModel.handleIntent(KLogViewerIntent.ShowSftpDialog) },
            onS3Click = { viewModel.handleIntent(KLogViewerIntent.ShowS3Dialog) },
            onAddFileClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDialog) },
            onAddDirectoryClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDirectoryDialog) },
            onAddSftpClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddSftpDialog) },
            onAddS3Click = { viewModel.handleIntent(KLogViewerIntent.ShowAddS3Dialog) },
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
            timeFilterFrom = activeWindow?.timeFilterFrom ?: "",
            timeFilterTo = activeWindow?.timeFilterTo ?: "",
            timeFilterPreset = activeWindow?.timeFilterPreset,
            timeFilterValidationMessage = activeWindow?.timeFilterValidationMessage,
            onTimeFilterFromChange = { viewModel.handleIntent(KLogViewerIntent.SetTimeFilterFrom(it)) },
            onTimeFilterToChange = { viewModel.handleIntent(KLogViewerIntent.SetTimeFilterTo(it)) },
            onApplyTimeFilterPreset = { preset ->
                viewModel.handleIntent(KLogViewerIntent.ApplyTimeFilterPreset(preset))
            },
            onClearTimeFilter = { viewModel.handleIntent(KLogViewerIntent.ClearTimeFilter) },
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
                    TextButton(
                        onClick = { viewModel.handleIntent(KLogViewerIntent.ShowLogs) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Logs",
                            fontWeight = if (window.workspaceMode == WorkspaceMode.LOGS) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(
                        onClick = { viewModel.handleIntent(KLogViewerIntent.ShowDashboard) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Dashboard",
                            fontWeight = if (window.workspaceMode == WorkspaceMode.DASHBOARD) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }

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
            } else if (window.filePath.isEmpty() && window.logs.isEmpty() && !window.isDirectory) {
                WelcomeScreen(
                    onOpenFile = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDialog) },
                    onOpenDirectory = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDirectoryDialog) },
                    onConnectSftp = { viewModel.handleIntent(KLogViewerIntent.ShowSftpDialog) },
                    onConnectS3 = { viewModel.handleIntent(KLogViewerIntent.ShowS3Dialog) },
                    onShowRecent = { viewModel.handleIntent(KLogViewerIntent.ShowRecentDialog) }
                )
            } else if (window.workspaceMode == WorkspaceMode.DASHBOARD) {
                DashboardWorkspace(window = window, viewModel = viewModel)
            } else {
                LogWorkspace(
                    window = window,
                    isWindowActive = isWindowActive,
                    isDarkMode = state.isDarkMode,
                    viewModel = viewModel
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
private fun LogWorkspace(
    window: LogWindow,
    isWindowActive: Boolean,
    isDarkMode: Boolean,
    viewModel: KLogViewerViewModel
) {
    val dashboardContent = logTimeFrequencyContent(window.dashboardDataState)

    Column(modifier = Modifier.fillMaxSize()) {
        dashboardContent?.let { content ->
            LogTimeFrequencyPanel(
                content = content,
                activeTimeFilterFrom = window.timeFilterFromInstant,
                activeTimeFilterTo = window.timeFilterToInstant,
                onBucketSelect = { bucket ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(bucket.from, bucket.to))
                },
                onBucketRangeSelect = { fromBucket, toBucket ->
                    viewModel.handleIntent(
                        KLogViewerIntent.SelectDashboardTimeRange(fromBucket.from, toBucket.to)
                    )
                },
                onLevelSelect = { level ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardLevel(level))
                },
                onFrequencyValueSelect = { value ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardFrequencyValue(value))
                },
                onClearTimeSelection = {
                    viewModel.handleIntent(KLogViewerIntent.ClearDashboardSelections)
                }
            )
        }

        LogList(
            logs = window.filteredLogs,
            filterQueries = window.filterQueries,
            isDarkMode = isDarkMode,
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
            },
            windowId = window.id,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LogTimeFrequencyPanel(
    content: DashboardDataState.Content,
    activeTimeFilterFrom: java.time.Instant?,
    activeTimeFilterTo: java.time.Instant?,
    onBucketSelect: (DashboardTimeBucket) -> Unit,
    onBucketRangeSelect: (DashboardTimeBucket, DashboardTimeBucket) -> Unit,
    onLevelSelect: (LogLevel) -> Unit,
    onFrequencyValueSelect: (String) -> Unit,
    onClearTimeSelection: () -> Unit
) {
    if (content.timeSeries.isEmpty()) {
        return
    }

    val activeTimeSelection = remember(
        content.timeSeries,
        content.selectedBucketFrom,
        activeTimeFilterFrom,
        activeTimeFilterTo
    ) {
        resolveDashboardTimeSelection(
            content = content,
            activeTimeFilterFrom = activeTimeFilterFrom,
            activeTimeFilterTo = activeTimeFilterTo
        )
    }

    Surface(
        color = MaterialTheme.colors.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time frequency",
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${content.totalEvents} events",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
                )
            }

            KoalaPlotTimeSeriesChart(
                buckets = content.timeSeries,
                bucketSize = content.bucketSize,
                selectedBucketFrom = content.selectedBucketFrom,
                selectedRangeFrom = activeTimeFilterFrom,
                selectedRangeTo = activeTimeFilterTo,
                onBucketSelect = onBucketSelect,
                onBucketRangeSelect = onBucketRangeSelect,
                chartHeight = 120.dp
            )

            DashboardActiveFilters(
                activeTimeSelection = activeTimeSelection,
                selectedLevel = content.selectedLevel,
                selectedFrequencyValue = content.selectedFrequencyValue,
                onClearTimeSelection = onClearTimeSelection,
                onClearLevel = {
                    content.selectedLevel?.let(onLevelSelect)
                },
                onClearFrequencyValue = {
                    content.selectedFrequencyValue?.let(onFrequencyValueSelect)
                }
            )
        }
    }
    Divider(modifier = Modifier.fillMaxWidth())
}

@Composable
private fun DashboardWorkspace(
    window: LogWindow,
    viewModel: KLogViewerViewModel
) {
    when (val dashboardState = window.dashboardDataState) {
        DashboardDataState.Empty -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No log events to analyze for the current filters.",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center
                )
            }
        }

        is DashboardDataState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dashboardState.message,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.handleIntent(KLogViewerIntent.ShowDashboard) }) {
                        Text("Retry")
                    }
                }
            }
        }

        DashboardDataState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DashboardDataState.Content -> {
            DashboardContent(
                content = dashboardState,
                activeTimeFilterFrom = window.timeFilterFromInstant,
                activeTimeFilterTo = window.timeFilterToInstant,
                onBucketSizeChange = { bucketSize ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardBucketSize(bucketSize))
                },
                onBucketSelect = { bucket ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardTimeRange(bucket.from, bucket.to))
                },
                onBucketRangeSelect = { fromBucket, toBucket ->
                    viewModel.handleIntent(
                        KLogViewerIntent.SelectDashboardTimeRange(fromBucket.from, toBucket.to)
                    )
                },
                onLevelSelect = { slice ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardLevel(slice.level))
                },
                onFrequencyFieldChange = { field ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyField(field))
                },
                onFrequencyTopNChange = { topN ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyTopN(topN))
                },
                onFrequencyThresholdChange = { threshold ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyThreshold(threshold))
                },
                onFrequencyCardinalityLimitChange = { limit ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardFrequencyCardinalityLimit(limit))
                },
                onFrequencyValueSelect = { value ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardFrequencyValue(value))
                },
                onCompareBaselineFromChange = { from ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineFrom(from))
                },
                onCompareBaselineToChange = { to ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareBaselineTo(to))
                },
                onCompareComparisonFromChange = { from ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonFrom(from))
                },
                onCompareComparisonToChange = { to ->
                    viewModel.handleIntent(KLogViewerIntent.SetDashboardCompareComparisonTo(to))
                },
                onRunComparison = { viewModel.handleIntent(KLogViewerIntent.RunDashboardComparison) },
                onClearComparison = { viewModel.handleIntent(KLogViewerIntent.ClearDashboardComparison) },
                onCompareLevelSelect = { level ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardLevel(level))
                },
                onCompareFrequencyValueSelect = { value ->
                    viewModel.handleIntent(KLogViewerIntent.SelectDashboardFrequencyValue(value))
                },
                onClearSelections = { viewModel.handleIntent(KLogViewerIntent.ClearDashboardSelections) }
            )
        }
    }
}

@Composable
private fun DashboardContent(
    content: DashboardDataState.Content,
    activeTimeFilterFrom: java.time.Instant?,
    activeTimeFilterTo: java.time.Instant?,
    onBucketSizeChange: (DashboardBucketSize) -> Unit,
    onBucketSelect: (DashboardTimeBucket) -> Unit,
    onBucketRangeSelect: (DashboardTimeBucket, DashboardTimeBucket) -> Unit,
    onLevelSelect: (DashboardLevelSlice) -> Unit,
    onFrequencyFieldChange: (String) -> Unit,
    onFrequencyTopNChange: (Int) -> Unit,
    onFrequencyThresholdChange: (Int) -> Unit,
    onFrequencyCardinalityLimitChange: (Int) -> Unit,
    onFrequencyValueSelect: (String) -> Unit,
    onCompareBaselineFromChange: (String) -> Unit,
    onCompareBaselineToChange: (String) -> Unit,
    onCompareComparisonFromChange: (String) -> Unit,
    onCompareComparisonToChange: (String) -> Unit,
    onRunComparison: () -> Unit,
    onClearComparison: () -> Unit,
    onCompareLevelSelect: (LogLevel) -> Unit,
    onCompareFrequencyValueSelect: (String) -> Unit,
    onClearSelections: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onBucketSizeChange(DashboardBucketSize.PER_SECOND) }) {
                    Text(
                        text = "Per second",
                        fontWeight = if (content.bucketSize == DashboardBucketSize.PER_SECOND) FontWeight.Bold else FontWeight.Normal
                    )
                }
                TextButton(onClick = { onBucketSizeChange(DashboardBucketSize.PER_MINUTE) }) {
                    Text(
                        text = "Per minute",
                        fontWeight = if (content.bucketSize == DashboardBucketSize.PER_MINUTE) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            OutlinedButton(onClick = onClearSelections) {
                Text("Clear selections")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Total events: ${content.totalEvents}",
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Time-series frequency",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val activeTimeSelection = remember(
            content.timeSeries,
            content.selectedBucketFrom,
            activeTimeFilterFrom,
            activeTimeFilterTo
        ) {
            resolveDashboardTimeSelection(
                content = content,
                activeTimeFilterFrom = activeTimeFilterFrom,
                activeTimeFilterTo = activeTimeFilterTo
            )
        }

        KoalaPlotTimeSeriesChart(
            buckets = content.timeSeries,
            bucketSize = content.bucketSize,
            selectedBucketFrom = content.selectedBucketFrom,
            selectedRangeFrom = activeTimeFilterFrom,
            selectedRangeTo = activeTimeFilterTo,
            onBucketSelect = onBucketSelect,
            onBucketRangeSelect = onBucketRangeSelect
        )

        DashboardActiveFilters(
            activeTimeSelection = activeTimeSelection,
            selectedLevel = content.selectedLevel,
            selectedFrequencyValue = content.selectedFrequencyValue,
            onClearTimeSelection = onClearSelections,
            onClearLevel = {
                content.selectedLevel?.let { level ->
                    onLevelSelect(
                        content.levelDistribution.firstOrNull { it.level == level }
                            ?: DashboardLevelSlice(level = level, count = 0, ratio = 0f)
                    )
                }
            },
            onClearFrequencyValue = {
                content.selectedFrequencyValue?.let(onFrequencyValueSelect)
            }
        )

        DashboardBucketKeyboardFallback(
            buckets = content.timeSeries,
            selectedBucketFrom = content.selectedBucketFrom,
            onBucketSelect = onBucketSelect
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Level distribution",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        DashboardLevelDistributionSection(
            slices = content.levelDistribution,
            selectedLevel = content.selectedLevel,
            onLevelSelect = onLevelSelect,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Frequency analysis",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (content.availableFrequencyFields.isEmpty()) {
            Text(
                text = "No structured fields available for frequency analysis.",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Field:",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                content.availableFrequencyFields.forEach { field ->
                    TextButton(onClick = { onFrequencyFieldChange(field) }) {
                        Text(
                            text = field,
                            fontWeight = if (content.selectedFrequencyField == field) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DashboardNumberControl(
                    label = "Top N",
                    value = content.frequencyTopN,
                    step = 1,
                    onChange = onFrequencyTopNChange
                )
                DashboardNumberControl(
                    label = "Threshold",
                    value = content.frequencyThreshold,
                    step = 1,
                    onChange = onFrequencyThresholdChange
                )
                DashboardNumberControl(
                    label = "Cardinality",
                    value = content.frequencyCardinalityLimit,
                    step = 10,
                    onChange = onFrequencyCardinalityLimitChange
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            content.frequencyItems.forEach { item ->
                val isSelected = content.selectedFrequencyValue == item.value
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onFrequencyValueSelect(item.value) },
                    backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.15f) else MaterialTheme.colors.surface
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.value,
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = item.count.toString(),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A/B comparison",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val comparisonState = content.comparisonState

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(text = "Baseline", style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = comparisonState.baselineRange.from,
                onValueChange = onCompareBaselineFromChange,
                label = { Text("From") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = comparisonState.baselineRange.to,
                onValueChange = onCompareBaselineToChange,
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            comparisonState.baselineRange.validationMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Comparison", style = MaterialTheme.typography.caption, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = comparisonState.comparisonRange.from,
                onValueChange = onCompareComparisonFromChange,
                label = { Text("From") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = comparisonState.comparisonRange.to,
                onValueChange = onCompareComparisonToChange,
                label = { Text("To") },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
            comparisonState.comparisonRange.validationMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRunComparison) {
                    Text("Run compare")
                }
                OutlinedButton(onClick = onClearComparison) {
                    Text("Clear compare")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Level deltas",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        if (comparisonState.levelDeltas.isEmpty()) {
            Text(
                text = "Enter both ranges and run compare to view deltas.",
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        } else {
            comparisonState.levelDeltas.forEach { delta ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clickable { onCompareLevelSelect(delta.level) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = delta.level.name, style = MaterialTheme.typography.body2)
                        Text(
                            text = "${delta.baselineCount} -> ${delta.comparisonCount} (${delta.delta})",
                            style = MaterialTheme.typography.caption
                        )
                        Text(
                            text = dashboardDirectionLabel(delta.direction),
                            color = dashboardDirectionColor(delta.direction),
                            style = MaterialTheme.typography.caption,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Top value deltas",
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        comparisonState.fieldDeltas.forEach { delta ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { onCompareFrequencyValueSelect(delta.value) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = delta.value,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${delta.count} (${delta.delta ?: 0})",
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = dashboardDirectionLabel(delta.direction),
                        color = dashboardDirectionColor(delta.direction),
                        style = MaterialTheme.typography.caption,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardLevelDistributionSection(
    slices: List<DashboardLevelSlice>,
    selectedLevel: LogLevel?,
    onLevelSelect: (DashboardLevelSlice) -> Unit,
    modifier: Modifier = Modifier
) {
    val orderedSlices = remember(slices) { orderedLevelDistributionSlices(slices) }

    Column(modifier = modifier.fillMaxWidth()) {
        KoalaPlotLevelDistributionChart(
            slices = orderedSlices,
            selectedLevel = selectedLevel,
            onLevelSelect = { level ->
                onLevelSelect(
                    orderedSlices.firstOrNull { it.level == level }
                        ?: DashboardLevelSlice(level = level, count = 0, ratio = 0f)
                )
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .testTag("dashboard_level_distribution_chart")
        )

        Spacer(modifier = Modifier.height(8.dp))

        orderedSlices.forEach { slice ->
            DashboardLevelDistributionRow(
                slice = slice,
                isSelected = selectedLevel == slice.level,
                onSelect = { onLevelSelect(slice) }
            )
        }
    }
}

@Composable
private fun DashboardLevelDistributionRow(
    slice: DashboardLevelSlice,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val levelColor = dashboardLevelColor(slice.level)
    val borderColor = when {
        isSelected -> MaterialTheme.colors.primary
        isHovered -> MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
        else -> MaterialTheme.colors.onSurface.copy(alpha = 0.14f)
    }
    val backgroundColor = when {
        isSelected -> MaterialTheme.colors.primary.copy(alpha = 0.14f)
        isHovered -> MaterialTheme.colors.onSurface.copy(alpha = 0.04f)
        else -> MaterialTheme.colors.surface
    }
    val percentageText = formatLevelDistributionPercentage(slice.ratio)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .hoverable(interactionSource)
            .focusable()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onSelect
            )
            .semantics(mergeDescendants = true) {
                selected = isSelected
                contentDescription = "${slice.level.name}: ${slice.count} events, $percentageText"
            }
            .testTag("dashboard_level_row_${slice.level.name.lowercase()}"),
        backgroundColor = backgroundColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(levelColor, CircleShape)
                    )
                    Text(
                        text = slice.level.name,
                        style = MaterialTheme.typography.body2,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Text(
                    text = "${slice.count} ($percentageText)",
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = slice.ratio.coerceIn(0f, 1f),
                color = levelColor,
                backgroundColor = levelColor.copy(alpha = if (isSelected) 0.28f else 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
private fun dashboardLevelColor(level: LogLevel): Color {
    val colors = KLogViewerTheme.logColors
    return when (level) {
        LogLevel.DEBUG -> colors.debug
        LogLevel.INFO -> colors.info
        LogLevel.WARN -> colors.warn
        LogLevel.ERROR -> colors.error
        LogLevel.FATAL -> colors.fatal
        LogLevel.UNKNOWN -> colors.unknown
    }
}


@Composable
private fun DashboardActiveFilters(
    activeTimeSelection: DashboardTimeSelection?,
    selectedLevel: LogLevel?,
    selectedFrequencyValue: String?,
    onClearTimeSelection: () -> Unit,
    onClearLevel: () -> Unit,
    onClearFrequencyValue: () -> Unit
) {
    val hasFilters = activeTimeSelection != null || selectedLevel != null || !selectedFrequencyValue.isNullOrBlank()
    if (!hasFilters) {
        return
    }

    Text(
        text = "Active filters",
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        activeTimeSelection?.let { timeSelection ->
            DashboardFilterChip(
                label = timeSelection.label,
                onRemove = onClearTimeSelection,
                removeActionDescription = "Remove active time filter",
                chipDescription = "Active time filter chip, ${timeSelection.label}"
            )
        }
        selectedLevel?.let { level ->
            DashboardFilterChip(label = "Level: ${level.name}", onRemove = onClearLevel)
        }
        selectedFrequencyValue?.takeIf { it.isNotBlank() }?.let { value ->
            DashboardFilterChip(label = "Field value: $value", onRemove = onClearFrequencyValue)
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun DashboardFilterChip(
    label: String,
    onRemove: () -> Unit,
    removeActionDescription: String = "Remove filter",
    chipDescription: String = label
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colors.primary.copy(alpha = 0.12f),
        modifier = Modifier
            .height(26.dp)
            .semantics { contentDescription = chipDescription }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = removeActionDescription,
                modifier = Modifier.size(14.dp).clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun DashboardBucketKeyboardFallback(
    buckets: List<DashboardTimeBucket>,
    selectedBucketFrom: java.time.Instant?,
    onBucketSelect: (DashboardTimeBucket) -> Unit
) {
    if (buckets.isEmpty()) {
        return
    }

    val selectedIndex = buckets.indexOfFirst { it.from == selectedBucketFrom }
    val anchorIndex = if (selectedIndex >= 0) selectedIndex else 0
    val previousIndex = (anchorIndex - 1).coerceAtLeast(0)
    val nextIndex = (anchorIndex + 1).coerceAtMost(buckets.lastIndex)
    val selectedLabel = buckets.getOrNull(anchorIndex)?.let(::formatDashboardBucketLabel) ?: "None"

    Text(
        text = "Keyboard fallback",
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = { onBucketSelect(buckets.first()) }) {
            Text("First bucket")
        }
        OutlinedButton(onClick = { onBucketSelect(buckets[previousIndex]) }) {
            Text("Previous")
        }
        OutlinedButton(onClick = { onBucketSelect(buckets[nextIndex]) }) {
            Text("Next")
        }
    }
    Text(
        text = "Current bucket: $selectedLabel",
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

internal data class DashboardTimeSelection(
    val label: String,
    val from: java.time.Instant,
    val to: java.time.Instant
)

internal fun resolveDashboardTimeSelection(
    content: DashboardDataState.Content,
    activeTimeFilterFrom: java.time.Instant?,
    activeTimeFilterTo: java.time.Instant?
): DashboardTimeSelection? {
    val selectedBucket = content.timeSeries.firstOrNull { it.from == content.selectedBucketFrom }
    if (selectedBucket != null) {
        return DashboardTimeSelection(
            label = "Bucket: ${selectedBucket.from} → ${selectedBucket.to}",
            from = selectedBucket.from,
            to = selectedBucket.to
        )
    }

    if (activeTimeFilterFrom == null || activeTimeFilterTo == null || activeTimeFilterFrom.isAfter(activeTimeFilterTo)) {
        return null
    }

    val selectedBuckets = content.timeSeries
        .asSequence()
        .filter { bucket -> bucket.from >= activeTimeFilterFrom && bucket.to <= activeTimeFilterTo }
        .toList()
    if (selectedBuckets.isEmpty()) {
        return null
    }

    val rangeFrom = selectedBuckets.first().from
    val rangeTo = selectedBuckets.last().to
    val prefix = if (selectedBuckets.size == 1) "Bucket" else "Range"
    return DashboardTimeSelection(
        label = "$prefix: $rangeFrom → $rangeTo",
        from = rangeFrom,
        to = rangeTo
    )
}


internal fun logTimeFrequencyContent(
    dashboardState: DashboardDataState
): DashboardDataState.Content? {
    return (dashboardState as? DashboardDataState.Content)
        ?.takeIf { it.timeSeries.isNotEmpty() }
}

private fun formatDashboardBucketLabel(bucket: DashboardTimeBucket): String {
    return "${bucket.from} → ${bucket.to} (${bucket.count})"
}

@Composable
private fun DashboardNumberControl(
    label: String,
    value: Int,
    step: Int,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: $value", style = MaterialTheme.typography.caption)
        TextButton(onClick = { onChange((value - step).coerceAtLeast(1)) }) { Text("-") }
        TextButton(onClick = { onChange(value + step) }) { Text("+") }
    }
}

private fun dashboardDirectionLabel(direction: DashboardDeltaDirection): String {
    return when (direction) {
        DashboardDeltaDirection.INCREASE -> "Increase"
        DashboardDeltaDirection.DECREASE -> "Decrease"
        DashboardDeltaDirection.UNCHANGED -> "Unchanged"
    }
}

@Composable
private fun dashboardDirectionColor(direction: DashboardDeltaDirection): Color {
    return when (direction) {
        DashboardDeltaDirection.INCREASE -> Color(0xFF2E7D32)
        DashboardDeltaDirection.DECREASE -> Color(0xFFC62828)
        DashboardDeltaDirection.UNCHANGED -> MaterialTheme.colors.onSurface.copy(alpha = 0.7f)
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
