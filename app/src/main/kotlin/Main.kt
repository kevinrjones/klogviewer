import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
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

        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp,
            position = WindowPosition(Alignment.Center)
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "LogViewer",
            state = windowState
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
