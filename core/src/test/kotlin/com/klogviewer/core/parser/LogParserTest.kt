package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogFailure
import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class LogParserTest {
    private val parser = SimpleLogParser()

    @Test
    fun `should parse a valid INFO log line`() {
        val line = "2024-05-12 10:00:00 [INFO] This is a log message"
        val result = parser.parse(line)

        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2024-05-12 10:00:00")
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("This is a log message")
        }
    }

    @Test
    fun `should parse a valid ERROR log line`() {
        val line = "2024-05-12 10:05:00 [ERROR] Something went wrong"
        val result = parser.parse(line)

        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.ERROR)
        }
    }

    @Test
    fun `should return ParsingError for invalid line`() {
        val line = "invalid log line"
        val result = parser.parse(line)

        expectThat(result.isLeft()).isTrue()
        result.onLeft { failure ->
            expectThat(failure).isEqualTo(LogFailure.ParsingError("Could not parse log line", line))
        }
    }

    @Test
    fun `should parse application log line with milliseconds and thread`() {
        val line = "2026-05-14 11:17:06.395 [main] INFO  Main - Starting KLogViewer application"
        val result = parser.parse(line)

        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2026-05-14 11:17:06.395")
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Main - Starting KLogViewer application")
        }
    }

    @Test
    fun `should parse log line with timezone offset`() {
        val line = "2026-05-08 00:27:56.321 +01:00 [INF] more stuff here"
        val result = parser.parse(line)

        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.fields["level"]).isEqualTo("[INF]")
            expectThat(entry.content.value).isEqualTo("more stuff here")
        }
    }

    @Test
    fun `should parse line with no space after offset correctly`() {
        val line = "2026-05-08 00:27:56.321 +01:00[INF] more stuff here"
        val result = parser.parse(line)
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.fields["level"]).isEqualTo("[INF]")
            expectThat(entry.content.value).isEqualTo("more stuff here")
        }
    }

    @Test
    fun `should parse debug line with thread metadata correctly`() {
        val line = "2026-05-12 10:15:03.752 [main] DEBUG more stuff here"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.DEBUG)
            expectThat(entry.fields["level"]).isEqualTo("DEBUG")
            expectThat(entry.content.value).isEqualTo("more stuff here")
        }
    }
}
