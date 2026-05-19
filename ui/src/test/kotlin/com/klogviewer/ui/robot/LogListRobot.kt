package com.klogviewer.ui.robot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.unit.Dp

class LogListRobot(composeTestRule: ComposeTestRule, private val windowId: String? = null) : BaseRobot(composeTestRule) {
    
    private fun matcher(tag: String): SemanticsMatcher {
        val tagMatcher = hasTestTag(tag)
        return if (windowId != null) {
            tagMatcher and hasAnyAncestor(hasTestTag("window_$windowId"))
        } else {
            tagMatcher
        }
    }

    fun assertLogCount(expectedCount: Int) {
        waitUntilExists("log_entry_row")
        composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)
            .assertCountEquals(expectedCount)
    }

    fun assertHasText(text: String) {
        val textMatcher = hasText(text)
        val finalMatcher = if (windowId != null) {
            textMatcher and hasAnyAncestor(hasTestTag("window_$windowId"))
        } else {
            textMatcher
        }
        composeTestRule.onNode(finalMatcher, useUnmergedTree = true).assertExists()
    }

    fun clickOnRow(index: Int) {
        composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)[index]
            .performClick()
    }

    fun clickOnRowWithModifiers(index: Int, shift: Boolean = false, meta: Boolean = false) {
        if (shift) composeTestRule.onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        if (meta) composeTestRule.onRoot().performKeyInput { keyDown(Key.MetaLeft) }
        
        composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)[index]
            .performMouseInput { click() }
        
        if (meta) composeTestRule.onRoot().performKeyInput { keyUp(Key.MetaLeft) }
        if (shift) composeTestRule.onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
    }

    fun assertRowSelected(index: Int, isSelected: Boolean = true) {
        composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)[index]
            .assert(if (isSelected) isSelected() else isNotSelected())
    }

    fun resizeColumn(column: String, offset: Float) {
        composeTestRule.onNode(matcher("resize_handle_$column"), useUnmergedTree = true)
            .performMouseInput {
                val start = center
                val end = Offset(start.x + offset, start.y)
                moveTo(start)
                press()
                moveTo(end)
                release()
            }
    }

    fun assertColumnWidth(column: String, expectedWidth: Dp) {
        composeTestRule.onNode(matcher("column_header_$column"), useUnmergedTree = true)
            .assertWidthIsEqualTo(expectedWidth)
    }
}

fun ComposeTestRule.logList(windowId: String? = null, block: LogListRobot.() -> Unit) = 
    LogListRobot(this, windowId).apply(block)
