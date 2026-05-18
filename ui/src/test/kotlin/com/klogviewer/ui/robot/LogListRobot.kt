package com.klogviewer.ui.robot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

class LogListRobot(composeTestRule: ComposeTestRule) : BaseRobot(composeTestRule) {
    
    fun assertLogCount(expectedCount: Int) {
        waitUntilExists("log_entry_row")
        composeTestRule.onAllNodesWithTag("log_entry_row", useUnmergedTree = true)
            .assertCountEquals(expectedCount)
    }

    fun assertHasText(text: String) {
        composeTestRule.onNodeWithText(text).assertExists()
    }

    fun clickOnRow(index: Int) {
        onNodeWithTag("log_list")
            .onChildAt(index)
            .performClick()
    }
}

fun ComposeTestRule.logList(block: LogListRobot.() -> Unit) = LogListRobot(this).apply(block)
