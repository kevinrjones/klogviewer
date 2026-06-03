package com.klogviewer.startup

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Files
import java.nio.file.Path

class MainMenuShortcutPlatformWiringTest {

    @Test
    fun `given main menu when configuring accelerators then it uses platform-aware shortcut mapping`() {
        val mainFile = Path.of("src/main/kotlin/Main.kt")
        val content = Files.readString(mainFile)

        expectThat(content).contains("val menuShortcuts = remember { menuShortcutSetForOs() }")
        expectThat(content).contains("shortcut = menuShortcuts.newTab")
        expectThat(content).contains("shortcut = menuShortcuts.closeTab")
        expectThat(content).contains("shortcut = menuShortcuts.copy")
    }
}
