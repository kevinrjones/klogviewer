package com.klogviewer.ui.components

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LogHighlighterTest {

    @Test
    fun `should highlight filter query`() {
        val text = "Hello World"
        val query = "hello"
        val result = LogHighlighter.highlight(text, listOf(query), true)
        
        expectThat(result.text).isEqualTo(text)
        // Check if a SpanStyle was added for the match
        expectThat(result.spanStyles.size).isEqualTo(1)
    }

    @Test
    fun `should highlight timestamp`() {
        val text = "2026-05-12 12:00:00.000 INFO message"
        val result = LogHighlighter.highlight(text, emptyList(), true)
        
        expectThat(result.spanStyles.size).isEqualTo(1)
    }

    @Test
    fun `should highlight IP address`() {
        val text = "Connection from 192.168.1.1"
        val result = LogHighlighter.highlight(text, emptyList(), true)
        
        expectThat(result.spanStyles.size).isEqualTo(1)
    }

    @Test
    fun `should highlight UUID`() {
        val text = "Request ID: 550e8400-e29b-41d4-a716-446655440000"
        val result = LogHighlighter.highlight(text, emptyList(), true)
        
        expectThat(result.spanStyles.size).isEqualTo(1)
    }
    @Test
    fun `should highlight multiple filter queries`() {
        val text = "Hello World"
        val queries = listOf("hello", "world")
        val result = LogHighlighter.highlight(text, queries, true)
        
        expectThat(result.spanStyles.size).isEqualTo(2)
    }

    @Test
    fun `should parse ANSI SGR colors and strip codes`() {
        val text = "\u001b[31mRed\u001b[0m Normal"
        val result = LogHighlighter.highlight(text, emptyList(), true, true)
        
        expectThat(result.text).isEqualTo("Red Normal")
        expectThat(result.spanStyles.size).isEqualTo(2)
    }

    @Test
    fun `should NOT parse ANSI colors when disabled`() {
        val text = "\u001b[31mRed\u001b[0m Normal"
        val result = LogHighlighter.highlight(text, emptyList(), true, false)
        
        expectThat(result.text).isEqualTo(text)
        expectThat(result.spanStyles.size).isEqualTo(0)
    }
}
