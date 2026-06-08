package com.klogviewer.ui.mvi

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LevelFilterKey
import com.klogviewer.domain.model.S3Config
import com.klogviewer.domain.model.SftpConfig
import java.time.Instant

sealed interface KLogViewerIntent {
    sealed interface WorkspaceIntent : KLogViewerIntent
    sealed interface UiToggleIntent : KLogViewerIntent
    sealed interface FilterIntent : KLogViewerIntent
    sealed interface DashboardIntent : KLogViewerIntent
    sealed interface TabWindowIntent : KLogViewerIntent
    sealed interface EntryIntent : KLogViewerIntent
    sealed interface DialogIntent : KLogViewerIntent
    sealed interface RecentItemsIntent : KLogViewerIntent
    sealed interface SftpIntent : KLogViewerIntent
    sealed interface S3Intent : KLogViewerIntent

    data class LoadFiles(val paths: List<String>) : WorkspaceIntent
    data class AddToWorkspace(val paths: List<String>) : WorkspaceIntent
    data class DropFilesOnLogView(val paths: List<String>) : WorkspaceIntent
    data class DropFilesOnTabBar(val paths: List<String>) : WorkspaceIntent
    data class SelectPath(val path: String) : WorkspaceIntent
    data object ClearLogs : WorkspaceIntent
    
    data object ToggleTheme : UiToggleIntent
    data object ToggleSidebar : UiToggleIntent
    data object ToggleSortOrder : UiToggleIntent
    data object ToggleAutoScroll : UiToggleIntent
    data object ToggleAnsiColors : UiToggleIntent
    data object ToggleConnection : UiToggleIntent
    data object RefreshConnection : UiToggleIntent
    
    data class AddFilterQuery(val query: String) : FilterIntent
    data class RemoveFilterQuery(val query: String) : FilterIntent
    data object ClearFilterQueries : FilterIntent
    data class ToggleLevel(val level: LevelFilterKey) : FilterIntent
    data object ToggleAllLevels : FilterIntent
    data class SetTimeFilterFrom(val from: String) : FilterIntent
    data class SetTimeFilterTo(val to: String) : FilterIntent
    data class ApplyTimeFilterPreset(val preset: TimeRangePreset) : FilterIntent
    data object ClearTimeFilter : FilterIntent
    data object ResetTimeFilter : FilterIntent

    data object ShowDashboard : DashboardIntent
    data object ShowLogs : DashboardIntent
    data class SetDashboardBucketSize(val bucketSize: DashboardBucketSize) : DashboardIntent
    data class SelectDashboardTimeBucket(val bucketFrom: Instant) : DashboardIntent
    data class SelectDashboardTimeRange(val from: Instant, val to: Instant) : DashboardIntent
    data class SelectDashboardLevel(val level: LogLevel) : DashboardIntent
    data class SetDashboardFrequencyField(val fieldKey: String) : DashboardIntent
    data class SetDashboardFrequencyTopN(val topN: Int) : DashboardIntent
    data class SetDashboardFrequencyThreshold(val threshold: Int) : DashboardIntent
    data class SetDashboardFrequencyCardinalityLimit(val limit: Int) : DashboardIntent
    data class SelectDashboardFrequencyValue(val value: String) : DashboardIntent
    data class SetDashboardCompareBaselineFrom(val from: String) : DashboardIntent
    data class SetDashboardCompareBaselineTo(val to: String) : DashboardIntent
    data class SetDashboardCompareComparisonFrom(val from: String) : DashboardIntent
    data class SetDashboardCompareComparisonTo(val to: String) : DashboardIntent
    data object RunDashboardComparison : DashboardIntent
    data object ClearDashboardComparison : DashboardIntent
    data object ClearDashboardSelections : DashboardIntent
    
    data class SelectEntry(val entry: com.klogviewer.domain.model.LogEntry?) : EntryIntent
    data class ToggleEntrySelection(val index: Int, val isShiftPressed: Boolean = false, val isMetaPressed: Boolean = false) : EntryIntent
    data class SetEntryDetailViewMode(val mode: LogEntryDetailViewMode) : EntryIntent
    data class ToggleStructuredPathExpansion(val path: String) : EntryIntent
    data class ToggleStructuredScalarExpansion(val path: String) : EntryIntent
    data class ToggleRawPayloadExpansion(val expanded: Boolean) : EntryIntent
    data class CopyStructuredText(val text: String) : EntryIntent
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
    data object ShowFontDialog : DialogIntent
    data class ApplyLogFont(val family: String, val sizeSp: Int) : DialogIntent
    data object ConfirmPlaintextSecretSave : DialogIntent
    data object DeclinePlaintextSecretSave : DialogIntent
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
    data class ToggleSourceVisibilityInActiveWindow(val sourcePath: String) : TabWindowIntent
    data class UpdateColumnWidth(val windowId: String, val column: String, val width: Int) : TabWindowIntent
    data class ChangeParser(val windowId: String, val parserName: String) : TabWindowIntent
}
