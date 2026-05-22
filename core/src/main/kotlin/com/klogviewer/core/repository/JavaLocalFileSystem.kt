package com.klogviewer.core.repository

import com.klogviewer.core.source.DirectoryScanner
import com.klogviewer.domain.repository.LocalFileSystem
import java.io.File

class JavaLocalFileSystem : LocalFileSystem {
    override fun exists(path: String): Boolean = File(path).exists()
    override fun isFile(path: String): Boolean = File(path).isFile
    override fun isDirectory(path: String): Boolean = File(path).isDirectory
    
    override fun listFiles(path: String, filters: List<String>): List<String> {
        return DirectoryScanner(filters).scan(path)
    }

    override fun readLines(path: String, limit: Int): List<String> {
        return File(path).useLines { it.take(limit).toList() }
    }

    override fun getName(path: String): String = File(path).name
}
