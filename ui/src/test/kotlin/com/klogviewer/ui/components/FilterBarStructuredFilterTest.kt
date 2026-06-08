package com.klogviewer.ui.components

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import com.klogviewer.ui.mvi.TimeRangePreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class FilterBarStructuredFilterTest {

    @Test
    fun `structured trigger opens dialog`() = runComposeUiTest {
        setFilterBarContent()

        onNodeWithTag("structured_filter_trigger").assertIsDisplayed().performClick()

        onNodeWithTag("structured_filter_field_input").assertIsDisplayed()
        onNodeWithTag("structured_filter_operator_trigger").assertIsDisplayed()
        onNodeWithTag("structured_filter_apply").assertIsDisplayed()
        onNodeWithTag("structured_filter_cancel").assertIsDisplayed()
    }

    @Test
    fun `apply builds and submits value-based query`() = runComposeUiTest {
        var submittedQuery: String? = null
        setFilterBarContent(onAddQuery = { submittedQuery = it })

        onNodeWithTag("structured_filter_trigger").performClick()
        onNodeWithTag("structured_filter_field_input").performTextInput("message")
        onNodeWithTag("structured_filter_value_input").performTextInput("timeout")
        onNodeWithTag("structured_filter_apply").performClick()

        assertEquals("field:message=\"timeout\"", submittedQuery)
        onNodeWithTag("structured_filter_field_input").assertDoesNotExist()
    }

    @Test
    fun `exists operator does not require value`() = runComposeUiTest {
        var submittedQuery: String? = null
        setFilterBarContent(onAddQuery = { submittedQuery = it })

        onNodeWithTag("structured_filter_trigger").performClick()
        onNodeWithTag("structured_filter_field_input").performTextInput("trace.id")
        onNodeWithTag("structured_filter_operator_trigger").performClick()
        onNodeWithTag("structured_filter_operator_exists").performClick()

        onNodeWithTag("structured_filter_value_input").assertDoesNotExist()
        onNodeWithTag("structured_filter_apply").assertIsEnabled().performClick()

        assertEquals("field:trace.id exists", submittedQuery)
    }

    @Test
    fun `apply is disabled when required inputs are missing`() = runComposeUiTest {
        setFilterBarContent()

        onNodeWithTag("structured_filter_trigger").performClick()
        onNodeWithTag("structured_filter_apply").assertIsNotEnabled()

        onNodeWithTag("structured_filter_field_input").performTextInput("StatusCode")
        onNodeWithTag("structured_filter_apply").assertIsNotEnabled()

        onNodeWithTag("structured_filter_value_input").performTextInput("500")
        onNodeWithTag("structured_filter_apply").assertIsEnabled()
    }

    @Test
    fun `cancel closes dialog without applying query`() = runComposeUiTest {
        var submittedQuery: String? = null
        setFilterBarContent(onAddQuery = { submittedQuery = it })

        onNodeWithTag("structured_filter_trigger").performClick()
        onNodeWithTag("structured_filter_field_input").performTextInput("message")
        onNodeWithTag("structured_filter_value_input").performTextInput("error")
        onNodeWithTag("structured_filter_cancel").performClick()

        assertNull(submittedQuery)
        onNodeWithTag("structured_filter_field_input").assertDoesNotExist()
    }

    private fun ComposeUiTest.setFilterBarContent(onAddQuery: (String) -> Unit = {}) {
        setContent {
            FilterBar(
                filterQueries = emptyList(),
                onAddQuery = onAddQuery,
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
                timeFilterPreset = TimeRangePreset.LAST_5_MINUTES,
                timeFilterValidationMessage = null,
                onApplyTimeFilterPreset = {},
                onClearTimeFilter = {},
                matchesCount = 0,
                totalCount = 0
            )
        }
    }
}