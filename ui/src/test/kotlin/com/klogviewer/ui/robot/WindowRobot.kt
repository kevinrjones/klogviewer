package com.klogviewer.ui.robot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

class WindowRobot(composeTestRule: ComposeTestRule, private val windowId: String) : BaseRobot(composeTestRule) {
    
    fun logList(block: LogListRobot.() -> Unit) {
        // Scoped LogListRobot
        LogListRobot(composeTestRule, windowId).apply(block)
    }

    fun assertIsActive() {
        onNodeWithText("Active")
            .assertExists()
            .assert(hasAnyAncestor(hasTestTag("window_$windowId")))
    }

    fun closeSplit() {
        onNodeWithContentDescription("Close Split")
            .assertExists()
            .assert(hasAnyAncestor(hasTestTag("window_$windowId")))
            .performClick()
    }
}

fun ComposeTestRule.window(windowId: String, block: WindowRobot.() -> Unit) = 
    WindowRobot(this, windowId).apply(block)
