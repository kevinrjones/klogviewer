package com.klogviewer.ui.mvi

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig

sealed interface KLogViewerIntent {
    sealed interface WorkspaceIntent : KLogViewerIntent
    sealed interface UiToggleIntent : KLogViewerIntent
    sealed interface FilterIntent : KLogViewerIntent
    sealed interface TabWindowIntent : KLogViewerIntent
    sealed interface EntryIntent : KLogViewerIntent
    sealed interface DialogIntent : KLogViewerIntent
    sealed interface RecentItemsIntent : KLogViewerIntent
    sealed interface SftpIntent : KLogViewerIntent
    sealed interface S3Intent : KLogViewerIntent

    data class LoadFiles(val paths: List<String>) : WorkspaceIntent
    data class AddToWorkspace(val paths: List<String>) : WorkspaceIntent
    data class SelectPath(val path: String) : WorkspaceIntent
    data object ClearLogs : WorkspaceIntent
    
    data object ToggleTheme : UiToggleIntent
    data object ToggleSidebar : UiToggleIntent
    data object ToggleSortOrder : UiToggleIntent
    data object ToggleAutoScroll : UiToggleIntent
    data object ToggleAnsiColors : UiToggleIntent
    data object ToggleConnection : UiToggleIntent
    
    data class AddFilterQuery(val query: String) : FilterIntent
    data class RemoveFilterQuery(val query: String) : FilterIntent
    data object ClearFilterQueries : FilterIntent
    data class ToggleLevel(val level: LogLevel) : FilterIntent
    data object ToggleAllLevels : FilterIntent
    
    data class SelectEntry(val entry: com.klogviewer.domain.model.LogEntry?) : EntryIntent
    data class ToggleEntrySelection(val index: Int, val isShiftPressed: Boolean = false, val isMetaPressed: Boolean = false) : EntryIntent
    data object CopySelected : EntryIntent
    
    // Dialogs
    data object ShowOpenDialog : DialogIntent
    data object ShowOpenDirectoryDialog : DialogIntent
    data object ShowAddDialog : DialogIntent
    data object ShowAddDirectoryDialog : DialogIntent
    data object ShowAddSftpDialog : DialogIntent
    data object ShowAddS3Dialog : DialogIntent
    data object ShowRecentDialog : DialogIntent
    data object ShowSftpDialog : DialogIntent
    data object ShowS3Dialog : DialogIntent
    data object DismissDialog : DialogIntent
    
    // SFTP
    data class ConnectSftp(
        val name: String,
        val host: String,
        val port: Int,
        val user: String,
        val auth: com.klogviewer.domain.model.SftpAuth,
        val path: String,
        val addToWorkspace: Boolean = false
    ) : SftpIntent
    data class ConnectMultipleSftp(
        val config: SftpConfig,
        val paths: List<String>,
        val addToWorkspace: Boolean = false
    ) : SftpIntent
    data class ConnectSftpDirectory(
        val config: SftpConfig,
        val path: String,
        val addToWorkspace: Boolean = false
    ) : SftpIntent
    data class BrowseSftp(val config: SftpConfig, val path: String) : SftpIntent
    data class NavigateRemote(val path: String) : SftpIntent
    data class SaveSftpConnection(val config: SftpConfig) : SftpIntent
    data class DeleteSftpConnection(val name: String) : SftpIntent
    
    // S3
    data class ConnectS3(
        val config: S3Config,
        val addToWorkspace: Boolean = false
    ) : S3Intent
    data class ConnectMultipleS3(
        val config: S3Config,
        val keys: List<String>,
        val addToWorkspace: Boolean = false
    ) : S3Intent
    data class ConnectS3Directory(
        val config: S3Config,
        val prefix: String,
        val addToWorkspace: Boolean = false
    ) : S3Intent
    data class BrowseS3(val config: S3Config, val prefix: String) : S3Intent
    data class SaveS3Connection(val config: S3Config) : S3Intent
    data class DeleteS3Connection(val name: String) : S3Intent
    
    // Recent Items
    data class RemoveRecentItem(val path: String) : RecentItemsIntent
    data object ClearMissingRecentItems : RecentItemsIntent
    
    // Tab Management
    data object AddTab : TabWindowIntent
    data class CloseTab(val id: String) : TabWindowIntent
    data class SwitchTab(val id: String) : TabWindowIntent
    
    // Split Management
    data object SplitHorizontal : TabWindowIntent
    data class CloseWindow(val id: String) : TabWindowIntent
    data class SwitchWindow(val id: String) : TabWindowIntent
    data class UpdateColumnWidth(val windowId: String, val column: String, val width: Int) : TabWindowIntent
    data class ChangeParser(val windowId: String, val parserName: String) : TabWindowIntent
}
