package com.logviewer.core.parser

import com.logviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class JsonLogParserTest {

    @Test
    fun `should parse standard JSON log entry`() {
        val parser = JsonLogParser()
        val json = """{"timestamp": "2024-05-14 15:24:08", "level": "INFO", "message": "Hello JSON"}"""
        
        val result = parser.parse(json)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2024-05-14 15:24:08")
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Hello JSON")
        }
    }

    @Test
    fun `should handle custom keys`() {
        val config = JsonMapping(
            timestampKey = "ts",
            levelKey = "lvl",
            contentKey = "msg"
        )
        val parser = JsonLogParser(config)
        val json = """{"ts": "2024-05-14", "lvl": "DEBUG", "msg": "Custom keys"}"""
        
        val result = parser.parse(json)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2024-05-14")
            expectThat(entry.level).isEqualTo(LogLevel.DEBUG)
            expectThat(entry.content.value).isEqualTo("Custom keys")
        }
    }

    @Test
    fun `should handle nested objects by serializing them`() {
        val parser = JsonLogParser()
        val json = """{"timestamp": "2024-05-14", "level": "INFO", "message": {"id": 123, "text": "nested"}}"""
        
        val result = parser.parse(json)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.content.value).isEqualTo("""{"id":123,"text":"nested"}""")
        }
    }
}
