package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.StructuredValue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
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

    @Test
    fun `should preserve structured payload and expose flattened paths`() {
        val parser = JsonLogParser()
        val json =
            """{"timestamp":"2024-05-14T15:24:08Z","level":"INFO","message":"ok","items":[{"id":"a"},{"id":"b"}],"user":{"id":123}}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val structuredData = entry.structuredData
            expectThat(structuredData).isNotNull()
            expectThat(structuredData?.rawPayload).isEqualTo(json)
            expectThat(structuredData?.flatPathIndex?.get("items[0].id")).isEqualTo(
                listOf(StructuredValue.StringValue("a"))
            )
            expectThat(structuredData?.flatPathIndex?.get("items[1].id")).isEqualTo(
                listOf(StructuredValue.StringValue("b"))
            )
            expectThat(structuredData?.flatPathIndex?.get("items[].id")).isEqualTo(
                listOf(
                    StructuredValue.StringValue("a"),
                    StructuredValue.StringValue("b")
                )
            )
            expectThat(structuredData?.flatPathIndex?.get("user.id")).isEqualTo(
                listOf(StructuredValue.NumberValue("123"))
            )
        }
    }

    @Test
    fun `should map explicit json null to structured null value`() {
        val parser = JsonLogParser()
        val json = """{"timestamp":"2024-05-14","level":"INFO","message":"ok","error":null}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.structuredData).isNotNull()
            expectThat(entry.structuredData?.flatPathIndex?.get("error")).isEqualTo(
                listOf(StructuredValue.NullValue)
            )
        }
    }
}
