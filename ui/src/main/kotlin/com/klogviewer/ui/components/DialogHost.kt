package com.klogviewer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.klogviewer.domain.model.SftpConfig
import com.klogviewer.ui.components.pattern.DirectoryMappingsDialog
import com.klogviewer.ui.components.pattern.PatternWizardDialog
import com.klogviewer.ui.mvi.DEFAULT_LOG_FONT_FAMILY
import com.klogviewer.ui.mvi.DEFAULT_LOG_FONT_SIZE_SP
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import com.klogviewer.ui.viewmodel.KLogViewerViewModel

@Composable
internal fun DialogHandler(
    state: KLogViewerState,
    viewModel: KLogViewerViewModel,
    dialogProvider: DialogProvider
) {
    val pendingDialog = state.pendingDialog

    val fileDialogTypes = setOf(
        KLogViewerState.DialogType.OPEN,
        KLogViewerState.DialogType.ADD
    )
    val directoryDialogTypes = setOf(
        KLogViewerState.DialogType.OPEN_DIRECTORY,
        KLogViewerState.DialogType.ADD_DIRECTORY
    )

    LaunchedEffect(pendingDialog) {
        if (pendingDialog in fileDialogTypes || pendingDialog in directoryDialogTypes) {
            val title = when (pendingDialog) {
                KLogViewerState.DialogType.OPEN -> "Select Log File"
                KLogViewerState.DialogType.OPEN_DIRECTORY -> "Select Log Directory"
                KLogViewerState.DialogType.ADD -> "Add Log File"
                KLogViewerState.DialogType.ADD_DIRECTORY -> "Add Log Directory"
                else -> return@LaunchedEffect
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

        if (pendingDialog == KLogViewerState.DialogType.FONT) {
            val activeWindow = state.activeTab?.activeWindow
            val selection = dialogProvider.showMonospacedFontDialog(
                title = "Select Log Font",
                initialFamily = activeWindow?.logFontFamily ?: DEFAULT_LOG_FONT_FAMILY,
                initialSizeSp = activeWindow?.logFontSizeSp ?: DEFAULT_LOG_FONT_SIZE_SP
            )

            viewModel.handleIntent(KLogViewerIntent.DismissDialog)

            if (selection != null) {
                viewModel.handleIntent(
                    KLogViewerIntent.ApplyLogFont(
                        family = selection.family,
                        sizeSp = selection.sizeSp
                    )
                )
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

    if (state.pendingDialog == KLogViewerState.DialogType.PATTERN_WIZARD || state.patternWizardState.isVisible) {
        PatternWizardDialog(
            state = state.patternWizardState,
            isDarkMode = state.isDarkMode,
            onIntent = viewModel::handleIntent
        )
    }

    if (state.pendingDialog == KLogViewerState.DialogType.DIRECTORY_MAPPINGS) {
        DirectoryMappingsDialog(
            mappings = state.directoryPatternMappings,
            isDarkMode = state.isDarkMode,
            onOpenInWizard = { directoryKey ->
                viewModel.handleIntent(KLogViewerIntent.OpenDirectoryMappingInWizard(directoryKey))
            },
            onDeleteMapping = { directoryKey ->
                viewModel.handleIntent(KLogViewerIntent.DeleteDirectoryPatternMapping(directoryKey))
            },
            onDismiss = { viewModel.handleIntent(KLogViewerIntent.DismissDialog) }
        )
    }
}
