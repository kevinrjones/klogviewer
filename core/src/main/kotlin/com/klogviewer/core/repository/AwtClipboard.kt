package com.klogviewer.core.repository

import com.klogviewer.domain.repository.Clipboard
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class AwtClipboard : Clipboard {
    override fun copy(text: String) {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
}
