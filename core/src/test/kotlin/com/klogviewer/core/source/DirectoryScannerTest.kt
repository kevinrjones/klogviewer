package com.klogviewer.core.source

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.hasSize
import java.io.File
import java.nio.file.Path

class DirectoryScannerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `should find all log files recursively`() {
        val subDir = File(tempDir.toFile(), "sub").apply { mkdir() }
        val log1 = File(tempDir.toFile(), "app.log").apply { writeText("log1") }
        val log2 = File(subDir, "error.log").apply { writeText("log2") }
        val csv1 = File(tempDir.toFile(), "data.csv").apply { writeText("not a log") }
        
        val scanner = DirectoryScanner()
        val files = scanner.scan(tempDir.toFile().absolutePath)
        
        expectThat(files)
            .hasSize(2)
            .containsExactlyInAnyOrder(log1.absolutePath, log2.absolutePath)
    }

    @Test
    fun `should respect custom filters`() {
        val log1 = File(tempDir.toFile(), "app.log").apply { writeText("log1") }
        val txt1 = File(tempDir.toFile(), "data.csv").apply { writeText("csv data") }
        
        val scanner = DirectoryScanner(listOf("*.csv"))
        val files = scanner.scan(tempDir.toFile().absolutePath)
        
        expectThat(files)
            .hasSize(1)
            .containsExactlyInAnyOrder(txt1.absolutePath)
    }
}
