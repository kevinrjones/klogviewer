package com.klogviewer.ui.menu

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut

data class MenuShortcutSet(
    val newTab: KeyShortcut,
    val closeTab: KeyShortcut,
    val copy: KeyShortcut,
    val modifierLabel: String
)

fun menuShortcutSetForOs(osName: String = System.getProperty("os.name")): MenuShortcutSet {
    val isMac = osName.lowercase().contains("mac")
    return if (isMac) {
        MenuShortcutSet(
            newTab = KeyShortcut(Key.N, meta = true),
            closeTab = KeyShortcut(Key.W, meta = true),
            copy = KeyShortcut(Key.C, meta = true),
            modifierLabel = "Cmd"
        )
    } else {
        MenuShortcutSet(
            newTab = KeyShortcut(Key.N, ctrl = true),
            closeTab = KeyShortcut(Key.F4, ctrl = true),
            copy = KeyShortcut(Key.C, ctrl = true),
            modifierLabel = "Ctrl"
        )
    }
}
