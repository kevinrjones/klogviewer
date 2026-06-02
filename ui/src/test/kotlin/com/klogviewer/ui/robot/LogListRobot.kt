package com.klogviewer.ui.robot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalTestApi::class)
class LogListRobot(composeTestRule: ComposeUiTest, private val windowId: String? = null) : BaseRobot(composeTestRule) {
    
    private fun matcher(tag: String): SemanticsMatcher {
        val tagMatcher = hasTestTag(tag)
        return if (windowId != null) {
            tagMatcher and hasAnyAncestor(hasTestTag("window_$windowId"))
        } else {
            tagMatcher
        }
    }

    private fun rowMatcher(index: Int): SemanticsMatcher {
        return matcher("log_entry_row_$index")
    }

    private fun waitForAnyRow() {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun lazyColumnMatcher(): SemanticsMatcher {
        val listTagMatcher = hasTestTag("log_lazy_column")
        return if (windowId != null) {
            listTagMatcher and hasAnyAncestor(hasTestTag("window_$windowId"))
        } else {
            listTagMatcher
        }
    }

    private fun scrollToRow(index: Int) {
        waitForAnyRow()
        composeTestRule.onNode(lazyColumnMatcher(), useUnmergedTree = true)
            .performScrollToIndex(index)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(rowMatcher(index), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    fun assertLogCount(expectedCount: Int) {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(matcher("log_entry_row"), useUnmergedTree = true)
                .fetchSemanticsNodes().size == expectedCount
        }
    }

    fun assertHasText(text: String) {
        val textMatcher = hasText(text)
        val finalMatcher = if (windowId != null) {
            textMatcher and hasAnyAncestor(hasTestTag("window_$windowId"))
        } else {
            textMatcher
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(finalMatcher, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(finalMatcher, useUnmergedTree = true).assertExists()
    }

    fun clickOnRow(index: Int) {
        scrollToRow(index)
        composeTestRule.onNode(rowMatcher(index), useUnmergedTree = true)
            .performClick()
    }

    fun rowBoundsInRoot(index: Int): Rect {
        scrollToRow(index)
        return composeTestRule.onNode(rowMatcher(index), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
    }

    fun rightClickOnRow(index: Int, xFraction: Float = 0.5f, yFraction: Float = 0.5f): Offset {
        scrollToRow(index)
        val rowBounds = composeTestRule.onNode(rowMatcher(index), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val rootBounds = composeTestRule.onRoot(useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        val visibleLeft = max(rowBounds.left, rootBounds.left)
        val visibleRight = min(rowBounds.right, rootBounds.right)
        val visibleTop = max(rowBounds.top, rootBounds.top)
        val visibleBottom = min(rowBounds.bottom, rootBounds.bottom)

        val clickPositionInRoot = Offset(
            x = visibleLeft + ((visibleRight - visibleLeft) * xFraction),
            y = visibleTop + ((visibleBottom - visibleTop) * yFraction)
        )

        composeTestRule.onRoot(useUnmergedTree = true).performMouseInput {
            rightClick(clickPositionInRoot)
        }

        return clickPositionInRoot
    }

    fun scrollHorizontallyBy(deltaX: Float) {
        composeTestRule.onNode(matcher("log_horizontal_scroll_container"), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(deltaX, 0f)
            }
    }

    fun contextMenuTopLeft(): Offset {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(matcher("log_context_menu"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        val menuBounds = composeTestRule.onNode(matcher("log_context_menu"), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        return Offset(menuBounds.left, menuBounds.top)
    }

    fun contextMenuBounds(): Rect {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(matcher("log_context_menu"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        return composeTestRule.onNode(matcher("log_context_menu"), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
    }

    fun clickContextCopy() {
        composeTestRule.onNode(matcher("log_context_menu_copy"), useUnmergedTree = true)
            .performClick()
    }

    fun clickContextRefresh() {
        composeTestRule.onNode(matcher("log_context_menu_refresh"), useUnmergedTree = true)
            .performClick()
    }

    fun clickContextClear() {
        composeTestRule.onNode(matcher("log_context_menu_clear"), useUnmergedTree = true)
            .performClick()
    }

    fun assertContextMenuVisible() {
        composeTestRule.onNode(matcher("log_context_menu"), useUnmergedTree = true).assertExists()
    }

    fun assertContextCopyEnabled(isEnabled: Boolean) {
        composeTestRule.onNode(matcher("log_context_menu_copy"), useUnmergedTree = true)
            .assert(if (isEnabled) isEnabled() else isNotEnabled())
    }

    fun assertContextRefreshEnabled(isEnabled: Boolean) {
        composeTestRule.onNode(matcher("log_context_menu_refresh"), useUnmergedTree = true)
            .assert(if (isEnabled) isEnabled() else isNotEnabled())
    }

    fun assertContextClearEnabled(isEnabled: Boolean) {
        composeTestRule.onNode(matcher("log_context_menu_clear"), useUnmergedTree = true)
            .assert(if (isEnabled) isEnabled() else isNotEnabled())
    }

    fun clickOnRowWithModifiers(index: Int, shift: Boolean = false, meta: Boolean = false) {
        scrollToRow(index)
        if (shift) composeTestRule.onRoot().performKeyInput { keyDown(Key.ShiftLeft) }
        if (meta) composeTestRule.onRoot().performKeyInput { keyDown(Key.MetaLeft) }
        
        composeTestRule.onNode(rowMatcher(index), useUnmergedTree = true)
            .performMouseInput { click() }
        
        if (meta) composeTestRule.onRoot().performKeyInput { keyUp(Key.MetaLeft) }
        if (shift) composeTestRule.onRoot().performKeyInput { keyUp(Key.ShiftLeft) }
    }

    fun assertRowSelected(index: Int, isSelected: Boolean = true) {
        scrollToRow(index)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val nodes = composeTestRule.onAllNodes(rowMatcher(index), useUnmergedTree = true).fetchSemanticsNodes()
            nodes.isNotEmpty() &&
                ((nodes.first().config.getOrElse(SemanticsProperties.Selected) { false }) == isSelected)
        }
        composeTestRule.onNode(rowMatcher(index), useUnmergedTree = true)
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
        val nodeMatcher = matcher("column_header_$column")
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(nodeMatcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNode(nodeMatcher, useUnmergedTree = true)
            .assertWidthIsEqualTo(expectedWidth)
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.logList(windowId: String? = null, block: LogListRobot.() -> Unit) = 
    LogListRobot(this, windowId).apply(block)
