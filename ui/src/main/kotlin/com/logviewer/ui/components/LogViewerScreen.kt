package com.logviewer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.logviewer.ui.mvi.LogViewerIntent
import com.logviewer.ui.viewmodel.LogViewerViewModel

@Composable
fun LogViewerScreen(viewModel: LogViewerViewModel) {
    val state by viewModel.state.collectAsState()
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
                }
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
