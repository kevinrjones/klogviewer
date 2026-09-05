package com.klogviewer.ui.components

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

@OptIn(ExperimentalTestApi::class)
class FilterBarToolbarChunkingTest {

    @Test
    fun `given filter bar when rendered then stream actions and overflow menu work`() = runComposeUiTest {
        var sidebarToggled = false
        var splitClicked = false
        var themeToggled = false
        var ansiToggled = false
        var compactToggled = false

        setContent {
            MaterialTheme {
                RenderTestFilterBar(
                    onToggleSidebar = { sidebarToggled = true },
                    onSplitClick = { splitClicked = true },
                    onToggleTheme = { themeToggled = true },
                    onToggleAnsi = { ansiToggled = true },
                    onToggleCompact = { compactToggled = true }
                )
            }
        }

        onNodeWithTag("toolbar_group_sources").assertIsDisplayed()
        onNodeWithTag("toolbar_group_stream").assertIsDisplayed()
        onNodeWithTag("toolbar_group_view").assertIsDisplayed()
        onNodeWithTag("toolbar_group_more").assertIsDisplayed()
        onNodeWithTag("toolbar_group_filters").assertIsDisplayed()
        onNodeWithTag("toolbar_open_file").assertIsDisplayed()
        onNodeWithTag("toggle_sidebar").assertIsDisplayed().performClick()
        expectThat(sidebarToggled).isTrue()

        onNodeWithTag("split_horizontal").assertIsDisplayed().performClick()
        expectThat(splitClicked).isTrue()

        onNodeWithTag("toolbar_more_menu").assertIsDisplayed().performClick()
        onNodeWithText("Toggle Theme").assertIsDisplayed().performClick()
        expectThat(themeToggled).isTrue()

        onNodeWithTag("toolbar_more_menu").performClick()
        onNodeWithText("ANSI Colors: ON").assertIsDisplayed().performClick()
        expectThat(ansiToggled).isTrue()

        onNodeWithTag("toolbar_more_menu").performClick()
        onNodeWithText("Cell View: Compact").assertIsDisplayed().performClick()
        expectThat(compactToggled).isTrue()
    }

    @Composable
    private fun RenderTestFilterBar(
        onToggleSidebar: () -> Unit,
        onSplitClick: () -> Unit,
        onToggleTheme: () -> Unit,
        onToggleAnsi: () -> Unit,
        onToggleCompact: () -> Unit
    ) {
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
            onToggleTheme = onToggleTheme,
            onToggleSidebar = onToggleSidebar,
            isReversed = false,
            onToggleSortOrder = {},
            isAutoScrollEnabled = true,
            onToggleAutoScroll = {},
            showAnsiColors = true,
            onToggleAnsiColors = onToggleAnsi,
            isConnected = true,
            onToggleConnection = {},
            onRefresh = {},
            onSplitClick = onSplitClick,
            onEditPatternMapping = null,
            timeFilterFrom = "",
            timeFilterTo = "",
            timeFilterPreset = null,
            timeFilterValidationMessage = null,
            onApplyTimeFilterPreset = {},
            onClearTimeFilter = {},
            matchesCount = 10,
            totalCount = 100,
            useCompactCellMode = true,
            onToggleCompactCellMode = onToggleCompact
        )
    }
}
