package com.klogviewer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.ui.mvi.TimeRangePreset
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FilterBarTimeFilterControlsTest {

    @Test
    fun `time filter renders direct inputs with placeholders and no dropdown options`() = runComposeUiTest {
        setupFilterBar()

        onNodeWithTag("time_filter_from_input").assertIsDisplayed()
        onNodeWithTag("time_filter_to_input").assertIsDisplayed()
        onNodeWithText("From").assertIsDisplayed()
        onNodeWithText("To").assertIsDisplayed()

        onNodeWithTag("time_filter_from_input").performClick()
        onNodeWithText("Any time").assertDoesNotExist()
        onNodeWithText("2026-05-26T10:00:00Z").assertDoesNotExist()
    }

    @Test
    fun `time filter clear from clear to and clear all actions update values`() = runComposeUiTest {
        var from by mutableStateOf("2026-05-26 10:00:00")
        var to by mutableStateOf("2026-05-26 10:05:00")

        setContent {
            FilterBar(
                filterQueries = emptyList(),
                onAddQuery = {},
                onRemoveQuery = {},
                onClearQueries = {},
                onOpenFileClick = {},
                onOpenDirectoryClick = {},
                onSftpClick = {},
                onS3Click = {},
                onAddFileClick = {},
                onAddDirectoryClick = {},
                onAddSftpClick = {},
                onAddS3Click = {},
                onToggleTheme = {},
                onToggleSidebar = {},
                isReversed = false,
                onToggleSortOrder = {},
                isAutoScrollEnabled = true,
                onToggleAutoScroll = {},
                showAnsiColors = true,
                onToggleAnsiColors = {},
                isConnected = true,
                onToggleConnection = {},
                onSplitClick = {},
                timeFilterFrom = from,
                timeFilterTo = to,
                timeFilterPreset = null,
                timeFilterValidationMessage = null,
                onTimeFilterFromChange = { from = it },
                onTimeFilterToChange = { to = it },
                onApplyTimeFilterPreset = {},
                onClearTimeFilter = {
                    from = ""
                    to = ""
                },
                matchesCount = 0,
                totalCount = 0
            )
        }

        onNodeWithTag("time_filter_clear").performClick()
        onNodeWithTag("time_filter_clear").assertDoesNotExist()

        onNodeWithTag("time_filter_from_input").performTextInput("2026-05-26 10:01:00")
        onNodeWithTag("time_filter_to_input").performTextInput("2026-05-26 10:06:00")

        onNodeWithTag("time_filter_clear_from").assertIsDisplayed()
        onNodeWithTag("time_filter_clear_to").assertIsDisplayed()
        onNodeWithTag("time_filter_clear_from").performClick()
        onNodeWithTag("time_filter_clear_from").assertDoesNotExist()
        onNodeWithTag("time_filter_clear_to").performClick()
        onNodeWithTag("time_filter_clear_to").assertDoesNotExist()
    }

    private fun ComposeUiTest.setupFilterBar() {
        var from by mutableStateOf("")
        var to by mutableStateOf("")

        setContent {
            FilterBar(
                filterQueries = emptyList(),
                onAddQuery = {},
                onRemoveQuery = {},
                onClearQueries = {},
                onOpenFileClick = {},
                onOpenDirectoryClick = {},
                onSftpClick = {},
                onS3Click = {},
                onAddFileClick = {},
                onAddDirectoryClick = {},
                onAddSftpClick = {},
                onAddS3Click = {},
                onToggleTheme = {},
                onToggleSidebar = {},
                isReversed = false,
                onToggleSortOrder = {},
                isAutoScrollEnabled = true,
                onToggleAutoScroll = {},
                showAnsiColors = true,
                onToggleAnsiColors = {},
                isConnected = true,
                onToggleConnection = {},
                onSplitClick = {},
                timeFilterFrom = from,
                timeFilterTo = to,
                timeFilterPreset = null,
                timeFilterValidationMessage = null,
                onTimeFilterFromChange = { from = it },
                onTimeFilterToChange = { to = it },
                onApplyTimeFilterPreset = {},
                onClearTimeFilter = {
                    from = ""
                    to = ""
                },
                matchesCount = 0,
                totalCount = 0
            )
        }
    }
}