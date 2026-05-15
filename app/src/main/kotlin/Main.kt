import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.klogviewer.core.parser.HeuristicProbe
import com.klogviewer.core.parser.ParserRegistry
import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.core.repository.PreferencesRepository
import com.klogviewer.core.source.FileLogSource
import com.klogviewer.domain.model.WindowStatePreferences
import com.klogviewer.ui.components.KLogViewerScreen
import com.klogviewer.ui.viewmodel.KLogViewerViewModel
import com.klogviewer.ui.mvi.KLogViewerIntent
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting KLogViewer application" }
    val prefsRepository = PreferencesRepository()
    val initialPrefs = prefsRepository.load()

    application {
        val parser = SimpleLogParser()
        val registry = ParserRegistry()
        val heuristicProbe = HeuristicProbe(registry)
        val source = FileLogSource(parser)
        val viewModel = KLogViewerViewModel(source, prefsRepository, heuristicProbe)

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
            prefsRepository.save(currentPrefs.copy(windowState = newWindowState))
            exitApplication()
        }

        Window(
            onCloseRequest = ::saveAndExit,
            title = "KLogViewer",
            state = windowState
        ) {
            MenuBar {
                Menu("File") {
                    Item("Open...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowOpenDialog) })
                    Item("Add to Workspace...", onClick = { viewModel.handleIntent(KLogViewerIntent.ShowAddDialog) })
                    
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
                    Item("New Tab", onClick = { viewModel.handleIntent(KLogViewerIntent.AddTab) })
                    Separator()
                    Item("Exit", onClick = ::saveAndExit)
                }
                Menu("Edit") {
                    Item("Clear Logs", onClick = { viewModel.handleIntent(KLogViewerIntent.ClearLogs) })
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
