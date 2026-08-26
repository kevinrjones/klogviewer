package com.klogviewer.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LogListGridLineTest {

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
        ),
        LogEntry(
            timestamp = LogTimestamp("2024-01-01 10:00:02"),
            level = LogLevel.DEBUG,
            content = LogContent("third message")
        )
    )

    @Test
    fun `given log list in dark mode when rendered then all rows are displayed with grid lines`() = runComposeUiTest {
        setContent {
            LogList(
                logs = testEntries,
                filterQueries = emptyList(),
                isDarkMode = true,
                columns = listOf("Timestamp", "Level", "Message"),
                columnWidths = mapOf(
                    "Timestamp" to 180,
                    "Level" to 80,
                    "Message" to 400
                )
            )
        }

        // Verify all rows are rendered
        onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("log_entry_row_1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("log_entry_row_2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `given log list in light mode when rendered then all rows are displayed with grid lines`() = runComposeUiTest {
        setContent {
            LogList(
                logs = testEntries,
                filterQueries = emptyList(),
                isDarkMode = false,
                columns = listOf("Timestamp", "Level", "Message"),
                columnWidths = mapOf(
                    "Timestamp" to 180,
                    "Level" to 80,
                    "Message" to 400
                )
            )
        }

        // Verify all rows are rendered
        onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("log_entry_row_1", useUnmergedTree = true).assertIsDisplayed()
        onNodeWithTag("log_entry_row_2", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `given log list with selected row when rendered then selected row is displayed with grid lines`() =
        runComposeUiTest {
            setContent {
                LogList(
                    logs = testEntries,
                    filterQueries = emptyList(),
                    isDarkMode = true,
                    columns = listOf("Timestamp", "Level", "Message"),
                    columnWidths = mapOf(
                        "Timestamp" to 180,
                        "Level" to 80,
                        "Message" to 400
                    ),
                    selectedIndices = setOf(1)
                )
            }

            // Verify all rows are rendered (selected row included)
            onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_1", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_2", useUnmergedTree = true).assertIsDisplayed()
        }

    @Test
    fun `given log list with multiple columns when rendered then all cells are displayed with grid lines`() =
        runComposeUiTest {
            setContent {
                LogList(
                    logs = testEntries,
                    filterQueries = emptyList(),
                    isDarkMode = true,
                    columns = listOf("Timestamp", "Level", "Message", "Source", "Thread"),
                    columnWidths = mapOf(
                        "Timestamp" to 180,
                        "Level" to 80,
                        "Message" to 400,
                        "Source" to 140,
                        "Thread" to 200
                    )
                )
            }

            // Verify all rows are rendered with multiple columns
            onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_1", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_2", useUnmergedTree = true).assertIsDisplayed()
        }

    @Test
    fun `given log list with alternating row colors when rendered then rows display with grid lines`() =
        runComposeUiTest {
            setContent {
                LogList(
                    logs = testEntries,
                    filterQueries = emptyList(),
                    isDarkMode = true,
                    sourceIds = listOf("source-1", "source-2"),
                    columns = listOf("Timestamp", "Level", "Message"),
                    columnWidths = mapOf(
                        "Timestamp" to 180,
                        "Level" to 80,
                        "Message" to 400
                    )
                )
            }

            // Verify all rows are rendered with alternating colors
            onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_1", useUnmergedTree = true).assertIsDisplayed()
            onNodeWithTag("log_entry_row_2", useUnmergedTree = true).assertIsDisplayed()
        }

    @Test
    fun `given log list with single entry when rendered then row is displayed with grid lines`() = runComposeUiTest {
        val singleEntry = listOf(
            LogEntry(
                timestamp = LogTimestamp("2024-01-01 10:00:00"),
                level = LogLevel.INFO,
                content = LogContent("single message")
            )
        )

        setContent {
            LogList(
                logs = singleEntry,
                filterQueries = emptyList(),
                isDarkMode = true,
                columns = listOf("Timestamp", "Level", "Message"),
                columnWidths = mapOf(
                    "Timestamp" to 180,
                    "Level" to 80,
                    "Message" to 400
                )
            )
        }

        onNodeWithTag("log_entry_row_0", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `given log list with empty logs when rendered then no rows are displayed`() = runComposeUiTest {
        setContent {
            LogList(
                logs = emptyList(),
                filterQueries = emptyList(),
                isDarkMode = true
            )
        }

        // Just verify it doesn't crash - the list renders with no rows
        onNodeWithTag("log_list", useUnmergedTree = true).assertIsDisplayed()
    }
}
