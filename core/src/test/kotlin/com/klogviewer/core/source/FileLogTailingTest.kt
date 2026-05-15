package com.klogviewer.core.source

import com.klogviewer.core.parser.SimpleLogParser
import com.klogviewer.domain.model.LogFilePath
import com.klogviewer.domain.model.LogUpdate
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class FileLogTailingTest {
    private val parser = SimpleLogParser()
    private val source = FileLogSource(parser)

    @Test
    fun `should tail log file appends`() = runBlocking {
        val file = File.createTempFile("tail-test", ".log").apply {
            writeText("2023-10-27 10:00:00 [INFO] Initial line\n")
            deleteOnExit()
        }

        val results = mutableListOf<LogUpdate>()
        val job = launch {
            source.observeLogs(LogFilePath(file.absolutePath)).collect { result ->
                result.map { results.add(it) }
            }
        }

        // Wait for initial load
        withTimeout(2000.milliseconds) {
            while (results.isEmpty()) delay(100.milliseconds)
        }
        assertEquals(1, (results[0] as LogUpdate.Initial).entries.size)

        // Append a line
        file.appendText("2023-10-27 10:00:01 [INFO] Appended line\n")

        // Wait for append
        withTimeout(3000.milliseconds) {
            while (results.size < 2) delay(100.milliseconds)
        }
        
        val appendUpdate = results[1] as LogUpdate.Appended
        assertEquals(1, appendUpdate.entries.size)
        assertEquals("Appended line", appendUpdate.entries[0].content.value)

        job.cancel()
    }
}
