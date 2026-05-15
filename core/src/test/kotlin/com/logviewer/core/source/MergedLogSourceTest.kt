package com.logviewer.core.source

import com.logviewer.core.parser.SimpleLogParser
import com.logviewer.domain.model.LogFilePath
import com.logviewer.domain.model.LogUpdate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class MergedLogSourceTest {
    private val parser = SimpleLogParser()
    private val source = FileLogSource(parser)

    @Test
    fun `should merge two log files chronologically`() = runBlocking {
        val file1 = File.createTempFile("log1", ".log").apply {
            writeText("""
                2023-10-27 10:00:00 [INFO] First log from file 1
                2023-10-27 10:00:02 [INFO] Second log from file 1
            """.trimIndent())
            deleteOnExit()
        }
        val file2 = File.createTempFile("log2", ".log").apply {
            writeText("""
                2023-10-27 10:00:01 [INFO] First log from file 2
                2023-10-27 10:00:03 [INFO] Second log from file 2
            """.trimIndent())
            deleteOnExit()
        }

        val mergedSource = MergedLogSource(listOf(
            Triple(source, LogFilePath(file1.absolutePath), null),
            Triple(source, LogFilePath(file2.absolutePath), null)
        ))

        val result = mergedSource.observeMerged().first()
        
        assertTrue(result.isRight())
        val update = result.getOrNull() as LogUpdate.Initial
        assertEquals(4, update.entries.size)
        
        assertEquals("2023-10-27 10:00:00", update.entries[0].timestamp.value)
        assertEquals(file1.name, update.entries[0].sourceId)
        
        assertEquals("2023-10-27 10:00:01", update.entries[1].timestamp.value)
        assertEquals(file2.name, update.entries[1].sourceId)
        
        assertEquals("2023-10-27 10:00:02", update.entries[2].timestamp.value)
        assertEquals(file1.name, update.entries[2].sourceId)
        
        assertEquals("2023-10-27 10:00:03", update.entries[3].timestamp.value)
        assertEquals(file2.name, update.entries[3].sourceId)
    }
}
