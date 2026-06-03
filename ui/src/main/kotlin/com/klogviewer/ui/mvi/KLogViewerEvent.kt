package com.klogviewer.ui.mvi

sealed interface KLogViewerEvent {
    data class ShowError(val message: String) : KLogViewerEvent
    data class ShowInfo(val message: String) : KLogViewerEvent
}
