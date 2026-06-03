package com.klogviewer.ui.test

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.ui.components.LogList
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LogListSourceBadgeTooltipTest {

    @Test
    fun `given multi source rows when hovering badges then filename tooltip is shown for each row`() = runComposeUiTest {
        val sourceA = "/tmp/services/api.log"
        val sourceB = "/var/log/database/db.log"
        val logs = listOf(
            createEntry(sourceA, "api-first"),
            createEntry(sourceB, "db-first")
        )

        setContent {
            LogList(
                logs = logs,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = listOf(sourceA, sourceB)
            )
        }

        assertBadgeTooltip(rowIndex = 0, expectedTooltip = "api.log")
        assertBadgeTooltip(rowIndex = 1, expectedTooltip = "db.log")
    }

    @Test
    fun `given source list changes and live updates when hovering badges then tooltip mapping stays correct`() = runComposeUiTest {
        val sourceA = "/tmp/services/api.log"
        val sourceB = "/var/log/database/db.log"
        val sourceC = "s3://prod-logs/cache/cache.log"

        var sourceIds by mutableStateOf(listOf(sourceA, sourceB))
        var logs by mutableStateOf(
            listOf(
                createEntry(sourceA, "api-before"),
                createEntry(sourceB, "db-before")
            )
        )

        setContent {
            LogList(
                logs = logs,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = sourceIds
            )
        }

        runOnIdle {
            sourceIds = listOf(sourceB, sourceC)
            logs = listOf(
                createEntry(sourceB, "db-after-1"),
                createEntry(sourceC, "cache-after"),
                createEntry(sourceB, "db-after-2")
            )
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("log_source_badge_2", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        assertBadgeTooltip(rowIndex = 1, expectedTooltip = "cache.log")
    }

    private fun ComposeUiTest.assertBadgeTooltip(rowIndex: Int, expectedTooltip: String) {
        onNodeWithTag("log_source_badge_$rowIndex", useUnmergedTree = true)
            .assertExists()
            .performMouseInput { moveTo(center) }

        waitUntil(timeoutMillis = 5_000) {
            try {
                onNodeWithTag("log_source_badge_tooltip_$rowIndex", useUnmergedTree = true)
                    .assertTextEquals(expectedTooltip)
                true
            } catch (_: Throwable) {
                false
            }
        }

        onNodeWithTag("log_source_badge_tooltip_$rowIndex", useUnmergedTree = true)
            .assertExists()
            .assertTextEquals(expectedTooltip)
    }

    private fun createEntry(sourceId: String, message: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("2024-01-01 10:00:00"),
        level = LogLevel.INFO,
        content = LogContent(message),
        sourceId = sourceId
    )
}