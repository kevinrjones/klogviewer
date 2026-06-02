import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.*
import com.klogviewer.core.source.*
import com.klogviewer.domain.model.WindowStatePreferences
import com.klogviewer.domain.repository.PreferencesSaveResult
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.mvi.KLogViewerIntent
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting KLogViewer application" }
    val prefsRepository = JsonPreferencesRepository()
    val initialPrefs = prefsRepository.load()

    application {
        val parser = remember { SimpleLogParser() }
        val registry = remember { ParserRegistry() }
        val heuristicProbe = remember { HeuristicProbe(registry) }
        val source = remember { FileLogSource(parser) }

        val factory = remember { DefaultLogSourceFactory() }
        val clipboard = remember { AwtClipboard() }
        val localFileSystem = remember { JavaLocalFileSystem() }
        val remoteFileSystem = remember { UnifiedRemoteFileSystem() }

        val viewModel = remember {
            KLogViewerViewModel(
                logSource = source,
                prefsRepository = prefsRepository,
                heuristicProbe = heuristicProbe,
                logSourceFactory = factory,
                clipboard = clipboard,
                localFileSystem = localFileSystem,
                remoteFileSystem = remoteFileSystem
            )
        }

        val windowState = rememberWindowState(
            width = initialPrefs.windowState.width.dp,
            height = initialPrefs.windowState.height.dp,
            position = if (initialPrefs.windowState.x != null && initialPrefs.windowState.y != null) {
                WindowPosition(initialPrefs.windowState.x!!.dp, initialPrefs.windowState.y!!.dp)
            } else {
                WindowPosition(Alignment.Center)
            },
            placement = if (initialPrefs.windowState.isMaximized) WindowPlacement.Maximized else WindowPlacement.Floating
        )

        val state by viewModel.state.collectAsState()
        val canCopySelection = state.activeTab?.activeWindow?.selectedIndices?.isNotEmpty() == true

        fun saveAndExit() {
            val currentPrefs = prefsRepository.load()
            val newWindowState = WindowStatePreferences(
                width = windowState.size.width.value.toInt(),
                height = windowState.size.height.value.toInt(),
                x = (windowState.position as? WindowPosition.Absolute)?.x?.value?.toInt(),
                y = (windowState.position as? WindowPosition.Absolute)?.y?.value?.toInt(),
                isMaximized = windowState.placement == WindowPlacement.Maximized
            )
            viewModel.savePreferences()
            when (prefsRepository.save(currentPrefs.copy(windowState = newWindowState))) {
                PreferencesSaveResult.Saved -> Unit
                PreferencesSaveResult.RequiresPlaintextSecretConfirmation -> {
                    logger.warn { "Skipped final preference save because secure credential storage requires interactive plaintext fallback consent" }
                }

                is PreferencesSaveResult.Failed -> {
                    logger.error { "Failed to persist final window state while exiting" }
                }
            }
            exitApplication()
        }

        Window(
            onCloseRequest = ::saveAndExit,
            title = "KLogViewer",
            state = windowState
        ) {
            MenuBar {
                Menu("File") {
                    Item("Open File...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDialog) })
                    Item("Open Directory...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDirectoryDialog) })
                    Separator()
                    Item("Connect to SFTP...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowSftpDialog) })
                    Item("Connect to S3...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowS3Dialog) })
                    Separator()
                    Item("Add Local File...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDialog) })
                    Item("Add Local Directory...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDirectoryDialog) })
                    Item("Add Remote SFTP...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddSftpDialog) })
                    Item("Add Remote S3...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddS3Dialog) })
                    
                    Menu("Recently Opened") {
                        if (state.recentFiles.isEmpty() && state.recentDirectories.isEmpty()) {
                            Item("No recent items", enabled = false) {}
                        }
                        
                        state.recentFiles.take(5).forEach { path ->
                            Item(path, onClick = { viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path))) })
                        }
                        
                        if (state.recentFiles.isNotEmpty() && state.recentDirectories.isNotEmpty()) {
                            Separator()
                        }
                        
                        state.recentDirectories.take(5).forEach { path ->
                            Item(path, onClick = { viewModel.handleIntent(KLogViewerIntent.LoadFiles(listOf(path))) })
                        }
                        
                        if (state.recentFiles.size > 5 || state.recentDirectories.size > 5) {
                            Separator()
                            Item("More...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowRecentDialog) })
                        }
                    }
                    
                    Separator()
                    Item("New Tab", shortcut = KeyShortcut(Key.N, meta = true), onClick = { viewModel.handleIntent(KLogViewerIntent.AddTab) })
                    Item("Close Tab", shortcut = KeyShortcut(Key.W, meta = true), onClick = { 
                        state.activeTabId?.let { viewModel.handleIntent(KLogViewerIntent.CloseTab(it)) }
                    })
                    Separator()
                    Item("Exit", onClick = ::saveAndExit)
                }
                Menu("Edit") {
                    Item(
                        "Copy",
                        enabled = canCopySelection,
                        shortcut = KeyShortcut(Key.C, meta = true),
                        onClick = { viewModel.handleIntent(KLogViewerIntent.CopySelected) }
                    )
                    Item("Font...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowFontDialog) })
                    Item("Clear", onClick = { viewModel.handleIntent(KLogViewerIntent.ClearTimeFilter) })
                }
                Menu("View") {
                    Item("Toggle Dark Mode", onClick = { viewModel.handleIntent(KLogViewerIntent.ToggleTheme) })
                    Item("Toggle Sidebar", onClick = { viewModel.handleIntent(KLogViewerIntent.ToggleSidebar) })
                }
            }
            KLogViewerScreen(viewModel)
        }
    }
}
