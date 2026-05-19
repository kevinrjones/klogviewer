package com.klogviewer.core.source

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

class DirectoryScanner(private val filters: List<String> = listOf("*.log", "*.txt")) {
    
    private val matchers: List<PathMatcher> = filters.map { 
        FileSystems.getDefault().getPathMatcher("glob:$it") 
    }

    fun scan(directoryPath: String): List<String> {
        val root = File(directoryPath)
        if (!root.exists() || !root.isDirectory) return emptyList()

        val discoveredFiles = mutableListOf<String>()
        scanRecursive(root, discoveredFiles)
        return discoveredFiles
    }

    private fun scanRecursive(directory: File, discoveredFiles: MutableList<String>) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanRecursive(file, discoveredFiles)
            } else {
                if (matchers.any { it.matches(file.toPath().fileName) }) {
                    discoveredFiles.add(file.absolutePath)
                }
            }
        }
    }
}
