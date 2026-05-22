package com.klogviewer.core.repository

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor

class AwtClipboardTest {
    @Test
    fun `should copy text to clipboard`() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            println("Skipping AwtClipboardTest in headless environment")
            return
        }
        
        val clipboard = AwtClipboard()
        val text = "test-clipboard-content-${System.currentTimeMillis()}"
        
        clipboard.copy(text)
        
        val content = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
        expectThat(content).isEqualTo(text)
    }
}
