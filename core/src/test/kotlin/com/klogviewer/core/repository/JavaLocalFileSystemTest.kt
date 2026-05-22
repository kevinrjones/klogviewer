package com.klogviewer.core.repository

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.File

class JavaLocalFileSystemTest {
    @TempDir
    lateinit var tempDir: File

    private val fs = JavaLocalFileSystem()

    @Test
    fun `exists should return true for existing file`() {
        val file = File(tempDir, "test.txt")
        file.writeText("content")
        expectThat(fs.exists(file.absolutePath)).isTrue()
    }

    @Test
    fun `exists should return false for non-existing file`() {
        val file = File(tempDir, "non-existing.txt")
        expectThat(fs.exists(file.absolutePath)).isFalse()
    }

    @Test
    fun `isFile should return true for file`() {
        val file = File(tempDir, "test.txt")
        file.writeText("content")
        expectThat(fs.isFile(file.absolutePath)).isTrue()
        expectThat(fs.isDirectory(file.absolutePath)).isFalse()
    }

    @Test
    fun `isDirectory should return true for directory`() {
        val dir = File(tempDir, "test-dir")
        dir.mkdir()
        expectThat(fs.isDirectory(dir.absolutePath)).isTrue()
        expectThat(fs.isFile(dir.absolutePath)).isFalse()
    }
}
