package com.klogviewer.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class CellValuePopupTest {

    @Test
    fun givenCellValuePopup_whenRendered_thenDisplaysValue() = runComposeUiTest {
        setContent {
            CellValuePopup(
                value = "test value content",
                columnName = "Message",
                onDismiss = {}
            )
        }

        onNodeWithTag("cell_value_popup").assertIsDisplayed()
        onNodeWithText("test value content").assertIsDisplayed()
        onNodeWithText("Message").assertIsDisplayed()
    }

    @Test
    fun givenCellValuePopup_whenRendered_thenCopyButtonIsDisplayed() = runComposeUiTest {
        setContent {
            CellValuePopup(
                value = "test value",
                columnName = "Message",
                onDismiss = {}
            )
        }

        onNodeWithTag("cell_value_popup_copy").assertIsDisplayed()
    }

    @Test
    fun givenCellValuePopup_whenRendered_thenCloseButtonIsDisplayed() = runComposeUiTest {
        setContent {
            CellValuePopup(
                value = "test value",
                columnName = "Message",
                onDismiss = {}
            )
        }

        onNodeWithTag("cell_value_popup_close").assertIsDisplayed()
    }

    @Test
    fun givenCellValuePopup_whenCloseButtonClicked_thenDismissIsCalled() = runComposeUiTest {
        var dismissed = false
        setContent {
            CellValuePopup(
                value = "test value",
                columnName = "Message",
                onDismiss = { dismissed = true }
            )
        }

        onNodeWithTag("cell_value_popup_close").performClick()
        assert(dismissed) { "Expected onDismiss to be called when close button is clicked" }
    }

    @Test
    fun givenCellValuePopup_whenRendered_thenContentAreaIsDisplayed() = runComposeUiTest {
        setContent {
            CellValuePopup(
                value = "multiline\ntext\nvalue",
                columnName = "Content",
                onDismiss = {}
            )
        }

        onNodeWithTag("cell_value_popup_content").assertIsDisplayed()
    }

    @Test
    fun givenCellValuePopup_whenRendered_thenDismissInstructionIsDisplayed() = runComposeUiTest {
        setContent {
            CellValuePopup(
                value = "test value",
                columnName = "Message",
                onDismiss = {}
            )
        }

        onNodeWithText("Click outside or press Escape to dismiss").assertIsDisplayed()
    }
}
