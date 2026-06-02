package com.klogviewer.startup

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import java.nio.file.Files
import java.nio.file.Path

class MainCompositionLifecycleTest {

    @Test
    fun `given main composition when creating view model then instance is remembered`() {
        val mainFile = Path.of("src/main/kotlin/Main.kt")
        val content = Files.readString(mainFile)

        expectThat(content).contains("val viewModel = remember {")
        expectThat(content).contains("KLogViewerViewModel(")
    }
}
