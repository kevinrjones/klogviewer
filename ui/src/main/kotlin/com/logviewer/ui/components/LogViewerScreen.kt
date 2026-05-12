package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import com.logviewer.ui.mvi.LogViewerIntent
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

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(title = { Text("LogViewer Walking Skeleton") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            FileSelector(
                path = state.filePath,
                onLoadClick = { path ->
                    viewModel.handleIntent(LogViewerIntent.LoadFile(path))
                },
                onBrowseClick = { showFileDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.error != null) {
                Text(text = "Error: ${state.error}", color = MaterialTheme.colors.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    LogList(logs = state.logs)
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
