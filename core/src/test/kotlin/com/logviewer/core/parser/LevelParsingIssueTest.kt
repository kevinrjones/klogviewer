package com.logviewer.core.parser

import com.logviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class LevelParsingIssueTest {

    private val parser = SimpleLogParser()

    @Test
    fun `should not lose bracketed PID when it is not a level`() {
        val line = "2026-05-15 12:00:00 [main] [97267] some message"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.UNKNOWN)
            // It should merge [main] and [97267] into content to avoid "labeling" them
            expectThat(entry.content.value).isEqualTo("[main] [97267] some message")
            expectThat(entry.fields.containsKey("metadata")).isEqualTo(false)
        }
    }
    
    @Test
    fun `should handle multiple bracketed items correctly`() {
        val line = "2026-05-15 12:00:00 [main] [97267] INFO some message"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("some message")
            // metadata should contain both [main] and [97267] because INFO was found via look-ahead
            expectThat(entry.fields["metadata"]).isEqualTo("main] [97267")
        }
    }
}
