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
import com.klogviewer.domain.model.StructuredLogData
import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.ui.components.LogList
import com.klogviewer.ui.components.SourceShadeIndexSemanticsKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

        val initialApiShadeIndex = rowShadeIndex(rowIndex = 0)
        val initialDbShadeIndex = rowShadeIndex(rowIndex = 1)
        assertNotEquals(initialApiShadeIndex, initialDbShadeIndex)

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

        val updatedDbFirstRowShadeIndex = rowShadeIndex(rowIndex = 0)
        val updatedCacheShadeIndex = rowShadeIndex(rowIndex = 1)
        val updatedDbSecondRowShadeIndex = rowShadeIndex(rowIndex = 2)

        assertEquals(initialDbShadeIndex, updatedDbFirstRowShadeIndex)
        assertEquals(initialDbShadeIndex, updatedDbSecondRowShadeIndex)
        assertNotEquals(updatedDbFirstRowShadeIndex, updatedCacheShadeIndex)
    }

    @Test
    fun `given mixed structured and plain rows when rendering list then only structured rows show marker`() = runComposeUiTest {
        val logs = listOf(
            createStructuredEntry(sourceId = "structured.log", message = "structured"),
            createEntry(sourceId = "plain.log", message = "plain")
        )

        setContent {
            LogList(
                logs = logs,
                filterQueries = emptyList(),
                isDarkMode = true,
                sourceIds = listOf("structured.log", "plain.log")
            )
        }

        onNodeWithTag("log_structured_badge_0", useUnmergedTree = true).assertExists()
        onNodeWithTag("log_structured_badge_1", useUnmergedTree = true).assertDoesNotExist()
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

    private fun ComposeUiTest.rowShadeIndex(rowIndex: Int): Int {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("log_entry_row_$rowIndex", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        return onNodeWithTag("log_entry_row_$rowIndex", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config[SourceShadeIndexSemanticsKey]
    }

    private fun createEntry(sourceId: String, message: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("2024-01-01 10:00:00"),
        level = LogLevel.INFO,
        content = LogContent(message),
        sourceId = sourceId
    )

    private fun createStructuredEntry(sourceId: String, message: String): LogEntry = LogEntry(
        timestamp = LogTimestamp("2024-01-01 10:00:00"),
        level = LogLevel.INFO,
        content = LogContent(message),
        sourceId = sourceId,
        structuredData = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "user" to StructuredValue.ObjectValue(
                        mapOf("id" to StructuredValue.NumberValue("1"))
                    )
                )
            )
        )
    )
}