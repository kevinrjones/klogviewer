import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.core.source.FileLogSource
import com.logviewer.ui.components.LogViewerScreen
import com.logviewer.ui.viewmodel.LogViewerViewModel

fun main() = application {
    val parser = SimpleLogParser()
    val source = FileLogSource(parser)
    val viewModel = LogViewerViewModel(source)

    Window(
        onCloseRequest = ::exitApplication,
        title = "LogViewer"
    ) {
        LogViewerScreen(viewModel)
    }
}
