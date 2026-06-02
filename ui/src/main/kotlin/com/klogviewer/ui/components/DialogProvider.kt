package com.klogviewer.ui.components

import java.awt.FileDialog
import java.awt.Font
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.GridLayout
import java.io.File
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

data class FontSelection(
    val family: String,
    val sizeSp: Int
)

interface DialogProvider {
    fun showOpenFileDialog(
        title: String,
        allowedExtensions: List<String> = emptyList()
    ): String?
    
    fun showOpenDirectoryDialog(
        title: String
    ): String?

    fun showMonospacedFontDialog(
        title: String,
        initialFamily: String,
        initialSizeSp: Int
    ): FontSelection?

    fun showMessageDialog(title: String, message: String)
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

    override fun showMonospacedFontDialog(
        title: String,
        initialFamily: String,
        initialSizeSp: Int
    ): FontSelection? {
        val monospacedFamilies = runCatching {
            GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .availableFontFamilyNames
                .asSequence()
                .filter { family -> isMonospacedFamily(family) }
                .sorted()
                .toList()
        }.getOrDefault(listOf("Monospaced"))

        if (monospacedFamilies.isEmpty()) {
            return null
        }

        val selectedInitialFamily = monospacedFamilies
            .firstOrNull { it.equals(initialFamily, ignoreCase = true) }
            ?: monospacedFamilies.first()

        val familyComboBox = JComboBox(monospacedFamilies.toTypedArray()).apply {
            selectedItem = selectedInitialFamily
        }
        val sizeSpinner = JSpinner(
            SpinnerNumberModel(initialSizeSp.coerceIn(8, 72), 8, 72, 1)
        )

        val panel = JPanel(GridLayout(2, 2, 8, 8)).apply {
            add(JLabel("Font"))
            add(familyComboBox)
            add(JLabel("Size"))
            add(sizeSpinner)
        }

        val result = JOptionPane.showConfirmDialog(
            parent,
            panel,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        )

        if (result != JOptionPane.OK_OPTION) {
            return null
        }

        val family = familyComboBox.selectedItem as? String ?: return null
        val size = (sizeSpinner.value as? Int)?.coerceIn(8, 72) ?: initialSizeSp.coerceIn(8, 72)
        return FontSelection(family = family, sizeSp = size)
    }

    override fun showMessageDialog(title: String, message: String) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE)
    }

    private fun isMonospacedFamily(family: String): Boolean {
        val fontMetrics = parent?.getFontMetrics(Font(family, Font.PLAIN, 14))
            ?: java.awt.Canvas().getFontMetrics(Font(family, Font.PLAIN, 14))
        val narrow = fontMetrics.charWidth('i')
        val wide = fontMetrics.charWidth('W')
        val digit = fontMetrics.charWidth('0')
        return narrow == wide && wide == digit
    }
}
