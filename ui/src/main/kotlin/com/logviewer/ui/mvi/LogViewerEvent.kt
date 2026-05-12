package com.logviewer.ui.mvi

sealed interface LogViewerEvent {
    data class ShowError(val message: String) : LogViewerEvent
}
