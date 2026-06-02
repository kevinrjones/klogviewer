package com.klogviewer.ui.components

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LogListFontStyleTest {

    @Test
    fun `given monospaced font name when style is created then monospace family is used`() {
        val style = createLogFontStyle(fontFamily = "Monospaced", fontSizeSp = 14)

        expectThat(style.fontFamily).isEqualTo(FontFamily.Monospace)
        expectThat(style.fontSize).isEqualTo(14.sp)
    }

    @Test
    fun `given unsupported family and out of range size when style is created then monospace family and clamped size are used`() {
        val style = createLogFontStyle(fontFamily = "Arial", fontSizeSp = 100)

        expectThat(style.fontFamily).isEqualTo(FontFamily.Monospace)
        expectThat(style.fontSize).isEqualTo(72.sp)
    }
}
