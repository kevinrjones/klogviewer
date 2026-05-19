package com.klogviewer.ui.robot

import androidx.compose.ui.test.*

@OptIn(ExperimentalTestApi::class)
class MainRobot(composeTestRule: ComposeUiTest) : BaseRobot(composeTestRule) {
    
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

    fun splitHorizontal() {
        onNodeWithTag("split_horizontal").performClick()
    }

    fun switchWindow(windowId: String) {
        onNodeWithTag("window_$windowId").performClick()
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.mainRobot(block: MainRobot.() -> Unit) = MainRobot(this).apply(block)
