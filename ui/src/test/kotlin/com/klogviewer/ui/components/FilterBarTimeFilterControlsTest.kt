package com.klogviewer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FilterBarTimeFilterControlsTest {

    @Test
    fun `time filter does not render from and to inputs`() = runComposeUiTest {
        setupFilterBar()

        onNodeWithTag("time_filter_from_input").assertDoesNotExist()
        onNodeWithTag("time_filter_to_input").assertDoesNotExist()
        onNodeWithText("From").assertDoesNotExist()
        onNodeWithText("To").assertDoesNotExist()
        onNodeWithTag("time_filter_preset").assertIsDisplayed()
    }

    @Test
    fun `time filter clear action clears active range`() = runComposeUiTest {
        var from by mutableStateOf("2026-05-26 10:00:00")
        var to by mutableStateOf("2026-05-26 10:05:00")
        var clearCalls = 0

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
                onApplyTimeFilterPreset = {},
                onClearTimeFilter = {
                    clearCalls += 1
                    from = ""
                    to = ""
                },
                matchesCount = 0,
                totalCount = 0
            )
        }

        onNodeWithTag("time_filter_clear").performClick()
        onNodeWithTag("time_filter_clear").assertDoesNotExist()
        assertEquals(1, clearCalls)
    }

    private fun ComposeUiTest.setupFilterBar() {
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
                timeFilterFrom = "",
                timeFilterTo = "",
                timeFilterPreset = null,
                timeFilterValidationMessage = null,
                onApplyTimeFilterPreset = {},
                onClearTimeFilter = {},
                matchesCount = 0,
                totalCount = 0
            )
        }
    }
}