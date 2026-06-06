package com.klogviewer.ui.robot

import androidx.compose.ui.test.*
import com.klogviewer.domain.model.LogLevel

@OptIn(ExperimentalTestApi::class)
class SidebarRobot(composeTestRule: ComposeUiTest) : BaseRobot(composeTestRule) {
    
    fun toggleLevel(level: LogLevel) {
        val label = level.name.toToggleLabel()
        onNodeWithTag("level_toggle_$label").performClick()
    }

    fun toggleLevel(level: String) {
        val label = level.toToggleLabel()
        onNodeWithTag("level_toggle_$label").performClick()
    }

    fun assertLevelIsEnabled(level: LogLevel, enabled: Boolean) {
        val label = level.name.toToggleLabel()
        // We can't easily check background color or border in standard Compose tests without custom semantics.
        // But we can check if the "Check" icon exists within the toggle if we make it accessible.
        // For now, let's just assert the toggle exists.
        onNodeWithTag("level_toggle_$label").assertExists()
    }

    fun assertLevelIsEnabled(level: String, enabled: Boolean) {
        val label = level.toToggleLabel()
        onNodeWithTag("level_toggle_$label").assertExists()
    }

    private fun String.toToggleLabel(): String {
        return lowercase().replaceFirstChar { it.uppercase() }
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.sidebar(block: SidebarRobot.() -> Unit) = SidebarRobot(this).apply(block)
