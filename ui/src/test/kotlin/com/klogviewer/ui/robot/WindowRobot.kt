package com.klogviewer.ui.robot

import androidx.compose.ui.test.*

@OptIn(ExperimentalTestApi::class)
class WindowRobot(composeTestRule: ComposeUiTest, private val windowId: String) : BaseRobot(composeTestRule) {
    
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

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.window(windowId: String, block: WindowRobot.() -> Unit) = 
    WindowRobot(this, windowId).apply(block)
