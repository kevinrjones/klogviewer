package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import com.logviewer.ui.mvi.LogViewerIntent
import com.logviewer.ui.theme.LogViewerTheme
import com.logviewer.ui.viewmodel.LogViewerViewModel
import java.awt.FileDialog
import java.awt.Frame

@Composable
fun LogViewerScreen(viewModel: LogViewerViewModel) {
    val state by viewModel.state.collectAsState()
    val scaffoldState = rememberScaffoldState()
    var showFileDialog by remember { mutableStateOf(false) }

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

    if (showFileDialog) {
        FileDialog(
            onCloseRequest = { result ->
                showFileDialog = false
                if (result != null) {
                    viewModel.handleIntent(LogViewerIntent.SelectPath(result))
                }
            }
        )
    }

    LogViewerTheme(darkTheme = state.isDarkMode) {
        Scaffold(
            scaffoldState = scaffoldState,
            bottomBar = {
                StatusBar(
                    filePath = state.filePath,
                    lineCount = state.logs.size
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
                    levelFilters = state.levelFilters,
                    onToggleLevel = { level -> viewModel.handleIntent(LogViewerIntent.ToggleLevel(level)) }
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Area
                    TopBar(
                        filePath = state.filePath,
                        onLoadClick = { path ->
                            viewModel.handleIntent(LogViewerIntent.LoadFile(path))
                        },
                        onBrowseClick = { showFileDialog = true },
                        searchQuery = state.searchQuery,
                        onSearchQueryChange = { query ->
                            viewModel.handleIntent(LogViewerIntent.UpdateSearch(query))
                        },
                        matchesCount = state.filteredLogs.size,
                        totalCount = state.logs.size
                    )

                    if (state.error != null) {
                        Text(
                            text = "Error: ${state.error}",
                            color = MaterialTheme.colors.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        } else {
                            LogList(
                                logs = state.filteredLogs,
                                searchQuery = state.searchQuery,
                                isDarkMode = state.isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileDialog(
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(null as Frame?, "Select Log File", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    if (directory != null && file != null) {
                        onCloseRequest(directory + file)
                    } else {
                        onCloseRequest(null)
                    }
                }
            }
        }
    },
    dispose = FileDialog::dispose
)
