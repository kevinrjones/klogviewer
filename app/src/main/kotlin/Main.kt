import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.core.service.LogService
import com.logviewer.ui.components.LogViewerScreen
import com.logviewer.ui.viewmodel.LogViewerViewModel

fun main() = application {
    val parser = SimpleLogParser()
    val service = LogService(parser)
    val viewModel = LogViewerViewModel(service)

    Window(
        onCloseRequest = ::exitApplication,
        title = "LogViewer Walking Skeleton"
    ) {
        MaterialTheme {
            LogViewerScreen(viewModel)
        }
    }
}
