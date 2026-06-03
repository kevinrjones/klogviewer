package com.klogviewer.ui.menu

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class MenuShortcutSetTest {

    @Test
    fun `given mac os name when resolving menu shortcuts then cmd accelerators are returned`() {
        val result = menuShortcutSetForOs("Mac OS X")

        expectThat(result.modifierLabel).isEqualTo("Cmd")
        expectThat(result.newTab).isEqualTo(KeyShortcut(Key.N, meta = true))
        expectThat(result.closeTab).isEqualTo(KeyShortcut(Key.W, meta = true))
        expectThat(result.copy).isEqualTo(KeyShortcut(Key.C, meta = true))
    }

    @Test
    fun `given windows os name when resolving menu shortcuts then ctrl accelerators are returned`() {
        val result = menuShortcutSetForOs("Windows 11")

        expectThat(result.modifierLabel).isEqualTo("Ctrl")
        expectThat(result.newTab).isEqualTo(KeyShortcut(Key.N, ctrl = true))
        expectThat(result.closeTab).isEqualTo(KeyShortcut(Key.F4, ctrl = true))
        expectThat(result.copy).isEqualTo(KeyShortcut(Key.C, ctrl = true))
    }

    @Test
    fun `given linux os name when resolving menu shortcuts then ctrl accelerators are returned`() {
        val result = menuShortcutSetForOs("Linux")

        expectThat(result.modifierLabel).isEqualTo("Ctrl")
        expectThat(result.newTab).isEqualTo(KeyShortcut(Key.N, ctrl = true))
        expectThat(result.closeTab).isEqualTo(KeyShortcut(Key.F4, ctrl = true))
        expectThat(result.copy).isEqualTo(KeyShortcut(Key.C, ctrl = true))
    }
}
