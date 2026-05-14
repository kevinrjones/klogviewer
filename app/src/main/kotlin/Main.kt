import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.core.source.FileLogSource
import com.logviewer.ui.components.LogViewerScreen
import com.logviewer.ui.viewmodel.LogViewerViewModel
import com.logviewer.ui.mvi.LogViewerIntent
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main() {
    logger.info { "Starting LogViewer application" }
    application {
        val parser = SimpleLogParser()
        val source = FileLogSource(parser)
        val viewModel = LogViewerViewModel(source)

        Window(
            onCloseRequest = ::exitApplication,
            title = "LogViewer"
        ) {
        MenuBar {
            Menu("File") {
                Item("Open...", onClick = { viewModel.handleIntent(LogViewerIntent.ShowOpenDialog) })
                Item("Add to Workspace...", onClick = { viewModel.handleIntent(LogViewerIntent.ShowAddDialog) })
                Separator()
                Item("New Tab", onClick = { viewModel.handleIntent(LogViewerIntent.AddTab) })
                Separator()
                Item("Exit", onClick = { exitApplication() })
            }
            Menu("Edit") {
                Item("Clear Logs", onClick = { viewModel.handleIntent(LogViewerIntent.ClearLogs) })
            }
            Menu("View") {
                Item("Toggle Dark Mode", onClick = { viewModel.handleIntent(LogViewerIntent.ToggleTheme) })
                Item("Toggle Sidebar", onClick = { viewModel.handleIntent(LogViewerIntent.ToggleSidebar) })
            }
        }
        LogViewerScreen(viewModel)
    }
}
}
