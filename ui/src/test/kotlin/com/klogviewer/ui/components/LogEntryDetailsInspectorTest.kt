package com.klogviewer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.StructuredLogData
import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.ui.mvi.LogEntryDetailViewMode
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LogEntryDetailsInspectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `given structured entry when expanding nested object then scalar child is shown`() {
        var expandedPaths by mutableStateOf(setOf<String>())

        composeTestRule.setContent {
            LogEntryDetails(
                entry = structuredEntry(),
                onClose = {},
                detailViewMode = LogEntryDetailViewMode.STRUCTURED,
                expandedStructuredPaths = expandedPaths,
                onToggleStructuredPathExpansion = {}
            )
        }

        composeTestRule.onNodeWithText("42", substring = true).assertDoesNotExist()
        composeTestRule.runOnIdle {
            expandedPaths = setOf("user")
        }
        composeTestRule.onNodeWithText("42", substring = true).assertExists()
    }

    @Test
    fun `given structured entry when toggling raw tab then raw payload is shown`() {
        var mode by mutableStateOf(LogEntryDetailViewMode.STRUCTURED)

        composeTestRule.setContent {
            LogEntryDetails(
                entry = structuredEntry(),
                onClose = {},
                detailViewMode = mode,
                onDetailViewModeChanged = { mode = it }
            )
        }

        composeTestRule.onNodeWithText("Structured payload").assertExists()
        composeTestRule.runOnIdle {
            mode = LogEntryDetailViewMode.RAW
        }
        composeTestRule.onNodeWithText("Raw payload").assertExists()
        composeTestRule.onNodeWithText("\"id\":42", substring = true).assertExists()
    }

    @Test
    fun `given structured scalar node when triggering actions then callbacks receive stable path and value`() {
        val copiedPaths = mutableListOf<String>()
        val copiedValues = mutableListOf<String>()
        val filteredFields = mutableListOf<String>()
        val filteredValues = mutableListOf<Pair<String, StructuredValue>>()

        composeTestRule.setContent {
            LogEntryDetails(
                entry = structuredEntry(),
                onClose = {},
                detailViewMode = LogEntryDetailViewMode.STRUCTURED,
                expandedStructuredPaths = setOf("user"),
                onCopyPath = { copiedPaths += it },
                onCopyValue = { copiedValues += it },
                onFilterByField = { filteredFields += it },
                onFilterByValue = { path, value -> filteredValues += path to value }
            )
        }

        composeTestRule.onNodeWithTag(copyPathActionTag("user")).performClick()
        composeTestRule.onNodeWithTag(copyValueActionTag("user.id")).performClick()
        composeTestRule.onNodeWithTag(filterFieldActionTag("user")).performClick()
        composeTestRule.onNodeWithTag(filterValueActionTag("user.id")).performClick()

        expectThat(copiedPaths.first()).isEqualTo("user")
        expectThat(copiedValues.first()).isEqualTo("42")
        expectThat(filteredFields.first()).isEqualTo("user")
        expectThat(filteredValues.first().first).isEqualTo("user.id")
        expectThat(filteredValues.first().second).isEqualTo(StructuredValue.NumberValue("42"))
    }

    @Test
    fun `given large raw payload when showing details then show more guardrail is available`() {
        val rawPayload = "{" + "\"data\":\"" + "x".repeat(60_000) + "\"}"
        var isExpanded by mutableStateOf(false)

        composeTestRule.setContent {
            LogEntryDetails(
                entry = structuredEntry(rawPayload = rawPayload),
                onClose = {},
                detailViewMode = LogEntryDetailViewMode.RAW,
                isRawPayloadExpanded = isExpanded,
                onRawPayloadExpansionChanged = { isExpanded = it }
            )
        }

        composeTestRule.onNodeWithText("Show more").assertExists()
        composeTestRule.runOnIdle {
            isExpanded = true
        }
        composeTestRule.onNodeWithText("Show less").assertExists()
    }

    @Test
    fun `given non structured entry when rendering details then content section remains unchanged`() {
        composeTestRule.setContent {
            LogEntryDetails(
                entry = plainEntry(),
                onClose = {}
            )
        }

        composeTestRule.onNodeWithText("Content").assertExists()
        composeTestRule.onNodeWithText("Structured").assertDoesNotExist()
    }

    private fun structuredEntry(rawPayload: String = "{\"user\":{\"id\":42}}") = LogEntry(
        timestamp = LogTimestamp("2026-06-08T10:00:00Z"),
        level = LogLevel.INFO,
        content = LogContent("structured payload"),
        structuredData = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "user" to StructuredValue.ObjectValue(
                        mapOf(
                            "id" to StructuredValue.NumberValue("42")
                        )
                    )
                )
            ),
            rawPayload = rawPayload
        )
    )

    private fun plainEntry() = LogEntry(
        timestamp = LogTimestamp("2026-06-08T10:00:00Z"),
        level = LogLevel.INFO,
        content = LogContent("plain payload")
    )
}
