package com.klogviewer.ui.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.geometry.Offset
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.ui.components.LogList
import com.klogviewer.ui.robot.logList
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import kotlin.math.abs

@OptIn(ExperimentalTestApi::class)
class LogListContextMenuTest {

    private val testEntries = listOf(
        LogEntry(
            timestamp = LogTimestamp("2024-01-01 10:00:00"),
            level = LogLevel.INFO,
            content = LogContent("first message")
        ),
        LogEntry(
            timestamp = LogTimestamp("2024-01-01 10:00:01"),
            level = LogLevel.ERROR,
            content = LogContent("second message")
        )
    )

    private fun createEntries(count: Int): List<LogEntry> = (0 until count).map { index ->
        LogEntry(
            timestamp = LogTimestamp("2024-01-01 10:00:${index.toString().padStart(2, '0')}"),
            level = if (index % 2 == 0) LogLevel.INFO else LogLevel.ERROR,
            content = LogContent("message-$index")
        )
    }

    @Test
    fun `given context menu actions when clicked then callbacks are invoked`() = runComposeUiTest {
        var copyInvocations = 0
        var refreshInvocations = 0
        var clearInvocations = 0

        setContent {
            LogList(
                logs = testEntries,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = listOf("source-1"),
                selectedIndices = setOf(0),
                onContextCopy = { copyInvocations += 1 },
                onContextRefresh = { refreshInvocations += 1 },
                onContextClear = { clearInvocations += 1 },
                isContextCopyEnabled = true,
                isContextRefreshEnabled = true,
                isContextClearEnabled = true
            )
        }

        logList {
            rightClickOnRow(0)
            assertContextMenuVisible()
            clickContextCopy()

            rightClickOnRow(0)
            clickContextRefresh()

            rightClickOnRow(0)
            clickContextClear()
        }

        expectThat(copyInvocations).isEqualTo(1)
        expectThat(refreshInvocations).isEqualTo(1)
        expectThat(clearInvocations).isEqualTo(1)
    }

    @Test
    fun `given no selection or source when context menu opens then copy and refresh are disabled but clear stays enabled`() = runComposeUiTest {
        setContent {
            LogList(
                logs = testEntries,
                filterQueries = emptyList(),
                isDarkMode = true,
                selectedIndices = emptySet(),
                sourceIds = emptyList(),
                isContextCopyEnabled = false,
                isContextRefreshEnabled = false,
                isContextClearEnabled = true
            )
        }

        logList {
            rightClickOnRow(0)
            assertContextMenuVisible()
            assertContextCopyEnabled(isEnabled = false)
            assertContextRefreshEnabled(isEnabled = false)
            assertContextClearEnabled(isEnabled = true)
        }
    }

    @Test
    fun `given row right click when context menu opens then menu aligns to click point`() = runComposeUiTest {
        var clickPosition = Offset.Zero
        var menuPosition = Offset.Zero

        setContent {
            LogList(
                logs = testEntries,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = listOf("source-1"),
                selectedIndices = setOf(0),
                isContextCopyEnabled = true,
                isContextRefreshEnabled = true,
                isContextClearEnabled = true
            )
        }

        logList {
            clickPosition = rightClickOnRow(index = 0, xFraction = 0.7f, yFraction = 0.5f)
            assertContextMenuVisible()
            menuPosition = contextMenuTopLeft()
        }

        expectThat(abs(menuPosition.x - clickPosition.x)).isLessThanOrEqualTo(3f)
    }

    @Test
    fun `given vertically and horizontally scrolled log list when row right clicked then menu keeps x alignment with click`() = runComposeUiTest {
        val scrolledEntries = createEntries(200)
        var clickPosition = Offset.Zero
        var menuPosition = Offset.Zero

        setContent {
            LogList(
                logs = scrolledEntries,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = listOf("source-1"),
                columns = listOf("Timestamp", "Level", "Message", "Thread"),
                columnWidths = mapOf(
                    "Timestamp" to 220,
                    "Level" to 120,
                    "Message" to 1800,
                    "Thread" to 300
                ),
                selectedIndices = setOf(150),
                isContextCopyEnabled = true,
                isContextRefreshEnabled = true,
                isContextClearEnabled = true
            )
        }

        logList {
            scrollHorizontallyBy(360f)
            clickPosition = rightClickOnRow(index = 150, xFraction = 0.7f, yFraction = 0.5f)
            assertContextMenuVisible()
            menuPosition = contextMenuTopLeft()
        }

        expectThat(abs(menuPosition.x - clickPosition.x)).isLessThanOrEqualTo(3f)
    }

}
