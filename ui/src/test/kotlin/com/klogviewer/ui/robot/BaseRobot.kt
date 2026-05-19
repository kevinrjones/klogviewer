package com.klogviewer.ui.robot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

abstract class BaseRobot(protected val composeTestRule: ComposeTestRule) {
    
    fun waitForIdle() {
        composeTestRule.waitForIdle()
    }

    protected fun onNodeWithTag(tag: String, useUnmergedTree: Boolean = false): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag(tag, useUnmergedTree)
    }

    protected fun onNodeWithText(text: String, useUnmergedTree: Boolean = false): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithText(text, useUnmergedTree)
    }

    protected fun onNodeWithContentDescription(description: String, useUnmergedTree: Boolean = false): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithContentDescription(description, useUnmergedTree)
    }

    fun assertExists(tag: String) {
        onNodeWithTag(tag).assertExists()
    }

    fun assertIsDisplayed(tag: String) {
        onNodeWithTag(tag).assertIsDisplayed()
    }

    fun waitUntilExists(tag: String, timeout: Long = 5000) {
        composeTestRule.waitUntil(timeout) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertDoesNotExist(tag: String) {
        onNodeWithTag(tag).assertDoesNotExist()
    }

    fun assertTextExists(text: String) {
        onNodeWithText(text).assertExists()
    }

    fun assertTextDoesNotExist(text: String) {
        onNodeWithText(text).assertDoesNotExist()
    }

    fun performMouseDrag(tag: String, startOffset: Offset, endOffset: Offset) {
        onNodeWithTag(tag).performMouseInput {
            moveTo(startOffset)
            press()
            moveTo(endOffset)
            release()
        }
    }

    fun clickWithModifiers(tag: String, shift: Boolean = false, meta: Boolean = false) {
        if (shift) composeTestRule.onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        if (meta) composeTestRule.onRoot().performKeyInput { keyDown(Key.MetaLeft) }
        
        onNodeWithTag(tag).performMouseInput { click() }
        
        if (meta) composeTestRule.onRoot().performKeyInput { keyUp(Key.MetaLeft) }
        if (shift) composeTestRule.onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
    }
}
