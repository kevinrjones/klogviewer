package com.klogviewer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        onNodeWithTag("time_filter_preset").performClick()
        onNodeWithTag("time_filter_clear_menu_item").assertIsDisplayed()
        onNodeWithText("Reset").assertIsDisplayed()
        onNodeWithText("Full loaded range").assertDoesNotExist()
    }

    @Test
    fun `time filter reset action clears active range`() = runComposeUiTest {
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
                onRefresh = {},
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

        onNodeWithTag("time_filter_preset").performClick()
        onNodeWithTag("time_filter_clear_menu_item").assertIsDisplayed().performClick()
        assertEquals(1, clearCalls)
    }

    @Test
    fun `refresh action is shown and triggers callback`() = runComposeUiTest {
        var refreshCalls = 0

        setContent {
            FilterBar(
                filterQueries = emptyList(),
                onAddQuery = {},
                onRemoveQuery = {},
                onClearQueries = {},
                onOpenFileClick = {},
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
                onRefresh = { refreshCalls += 1 },
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

        onNodeWithTag("toolbar_refresh").assertIsDisplayed().performClick()
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `time filter preset menu items use compact spacing`() = runComposeUiTest {
        setupFilterBar()

        onNodeWithTag("time_filter_preset").performClick()

        val fiveMinuteBounds = onNodeWithText("Last 5 minutes", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val fifteenMinuteBounds = onNodeWithText("Last 15 minutes", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val thirtyMinuteBounds = onNodeWithText("Last 30 minutes", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot

        val firstStep = fifteenMinuteBounds.top - fiveMinuteBounds.top
        val secondStep = thirtyMinuteBounds.top - fifteenMinuteBounds.top

        assertTrue(firstStep in 1f..36f, "Expected compact spacing between first and second preset items, was $firstStep")
        assertTrue(secondStep in 1f..36f, "Expected compact spacing between second and third preset items, was $secondStep")
    }

    private fun ComposeUiTest.setupFilterBar() {
        setContent {
            FilterBar(
                filterQueries = emptyList(),
                onAddQuery = {},
                onRemoveQuery = {},
                onClearQueries = {},
                onOpenFileClick = {},
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
                onRefresh = {},
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