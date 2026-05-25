package com.klogviewer.ui.viewmodel

import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.mvi.KLogViewerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class DialogIntentHandler(
    private val state: MutableStateFlow<KLogViewerState>
) {
    fun handle(intent: KLogViewerIntent.DialogIntent) {
        when (intent) {
            KLogViewerIntent.ShowOpenDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN, isAddMode = false) }
            KLogViewerIntent.ShowOpenDirectoryDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.OPEN_DIRECTORY, isAddMode = false) }
            KLogViewerIntent.ShowAddDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD, isAddMode = true) }
            KLogViewerIntent.ShowAddDirectoryDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.ADD_DIRECTORY, isAddMode = true) }
            KLogViewerIntent.ShowAddSftpDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_ADD, isAddMode = true) }
            KLogViewerIntent.ShowAddS3Dialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.S3_ADD, isAddMode = true) }
            KLogViewerIntent.ShowRecentDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.RECENT_ITEMS) }
            KLogViewerIntent.ShowSftpDialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.SFTP_CONNECT, isAddMode = false) }
            KLogViewerIntent.ShowS3Dialog -> state.update { it.copy(pendingDialog = KLogViewerState.DialogType.S3_CONNECT, isAddMode = false) }
            KLogViewerIntent.ConfirmPlaintextSecretSave -> Unit
            KLogViewerIntent.DeclinePlaintextSecretSave -> Unit
            KLogViewerIntent.DismissDialog -> state.update { it.copy(pendingDialog = null) }
        }
    }
}
