package com.klogviewer.ui.components

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

interface DialogProvider {
    fun showOpenFileDialog(
        title: String,
        allowedExtensions: List<String> = emptyList()
    ): String?
    
    fun showOpenDirectoryDialog(
        title: String
    ): String?
}

class AwtDialogProvider(private val parent: Frame? = null) : DialogProvider {
    override fun showOpenFileDialog(title: String, allowedExtensions: List<String>): String? {
        val dialog = FileDialog(parent, title, FileDialog.LOAD)
        if (allowedExtensions.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                allowedExtensions.any { name.endsWith(it, ignoreCase = true) }
            }
        }
        dialog.isVisible = true
        
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file).absolutePath
        } else {
            null
        }
    }

    override fun showOpenDirectoryDialog(title: String): String? {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        if (isMac) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
        }
        
        val dialog = FileDialog(parent, title, FileDialog.LOAD)
        dialog.isVisible = true
        
        if (isMac) {
            System.setProperty("apple.awt.fileDialogForDirectories", "false")
        }
        
        return if (dialog.directory != null && dialog.file != null) {
            File(dialog.directory, dialog.file).absolutePath
        } else if (dialog.directory != null) {
            File(dialog.directory).absolutePath
        } else {
            null
        }
    }
}
