package com.klogviewer.ui.components

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

interface DialogProvider {
    fun showOpenFileDialog(
        title: String,
        allowedExtensions: List<String> = emptyList()
    ): File?
}

class AwtDialogProvider(private val parent: Frame? = null) : DialogProvider {
    override fun showOpenFileDialog(title: String, allowedExtensions: List<String>): File? {
        val dialog = FileDialog(parent, title, FileDialog.LOAD)
        if (allowedExtensions.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                allowedExtensions.any { name.endsWith(it, ignoreCase = true) }
            }
        }
        dialog.isVisible = true
        
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file)
        } else {
            null
        }
    }
}
