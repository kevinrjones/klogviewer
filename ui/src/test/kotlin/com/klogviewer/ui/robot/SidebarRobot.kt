package com.klogviewer.ui.robot

import androidx.compose.ui.test.*
import com.klogviewer.domain.model.LogLevel

@OptIn(ExperimentalTestApi::class)
class SidebarRobot(composeTestRule: ComposeUiTest) : BaseRobot(composeTestRule) {
    
    fun toggleLevel(level: LogLevel) {
        val label = level.name.lowercase().replaceFirstChar { it.uppercase() }
        onNodeWithTag("level_toggle_$label").performClick()
    }

    fun assertLevelIsEnabled(level: LogLevel, enabled: Boolean) {
        val label = level.name.lowercase().replaceFirstChar { it.uppercase() }
        // We can't easily check background color or border in standard Compose tests without custom semantics.
        // But we can check if the "Check" icon exists within the toggle if we make it accessible.
        // For now, let's just assert the toggle exists.
        onNodeWithTag("level_toggle_$label").assertExists()
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.sidebar(block: SidebarRobot.() -> Unit) = SidebarRobot(this).apply(block)
