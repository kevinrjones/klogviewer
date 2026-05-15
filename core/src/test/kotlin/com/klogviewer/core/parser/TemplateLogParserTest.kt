package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class TemplateLogParserTest {

    @Test
    fun `should parse line using template with standard levels`() {
        val template = LogTemplate(
            name = "Standard",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+(?<level>\S+)\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd HH:mm:ss"
        )
        val parser = TemplateLogParser(template)
        
        val line = "2024-05-14 15:24:08 INFO Application started"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.timestamp.value).isEqualTo("2024-05-14 15:24:08")
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Application started")
        }
    }

    @Test
    fun `should use level mapper for abbreviated levels`() {
        val template = LogTemplate(
            name = "Abbreviated",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+(?<level>\S+)\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd HH:mm:ss",
            levelMapper = LevelMapper() // Default LevelMapper supports abbreviations
        )
        val parser = TemplateLogParser(template)
        
        val line = "2024-05-14 15:24:08 INF User logged in"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
        }
    }

    @Test
    fun `should handle missing optional groups`() {
        val template = LogTemplate(
            name = "No Level",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd HH:mm:ss"
        )
        val parser = TemplateLogParser(template)
        
        val line = "2024-05-14 15:24:08 Just a message without level"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.UNKNOWN)
            expectThat(entry.content.value).isEqualTo("Just a message without level")
        }
    }

    @Test
    fun `should try metadata group if level group is unknown`() {
        val template = LogTemplate(
            name = "MetadataTest",
            regex = """^(?<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+\[(?<metadata>.*?)\]\s+(?<level>\S+)\s+(?<content>.*)$""",
            timestampPattern = "yyyy-MM-dd HH:mm:ss"
        )
        val parser = TemplateLogParser(template)
        
        val line = "2024-05-14 15:24:08 [INFO] MyMethod - Message"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("MyMethod - Message")
        }
    }
}
