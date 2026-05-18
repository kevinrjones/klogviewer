package com.klogviewer.ui.robot

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

class MainRobot(composeTestRule: ComposeTestRule) : BaseRobot(composeTestRule) {
    
    fun clickAddTab() {
        onNodeWithTag("add_tab_button").performClick()
    }

    fun typeFilter(text: String) {
        onNodeWithTag("filter_input").performTextInput(text)
        onNodeWithTag("filter_input").performImeAction()
    }

    fun clearFilter() {
        onNodeWithTag("clear_all_filters").performClick()
    }

    fun clickAddFile() {
        onNodeWithTag("add_file_to_workspace").performClick()
    }
}

fun ComposeTestRule.mainRobot(block: MainRobot.() -> Unit) = MainRobot(this).apply(block)
