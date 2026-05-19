package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class ParserRegistryTest {

    private val registry = ParserRegistry()

    @Test
    fun `should have default templates`() {
        expectThat(registry.getTemplate("Standard")).isNotNull()
        expectThat(registry.getTemplate("Syslog")).isNotNull()
    }

    @Test
    fun `should register and retrieve custom template`() {
        val custom = LogTemplate("Custom", "^(?<content>.*)$", "")
        registry.register(custom)
        expectThat(registry.getTemplate("Custom")).isEqualTo(custom)
    }

    @Test
    fun `should return parser for registered template`() {
        val parser = registry.getParser("Standard")
        expectThat(parser).isNotNull()
        
        val line = "2024-05-14 15:24:08 INFO Hello"
        val result = parser!!.parse(line)
        expectThat(result.isRight()).isEqualTo(true)
        result.map { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Hello")
        }
    }
}
