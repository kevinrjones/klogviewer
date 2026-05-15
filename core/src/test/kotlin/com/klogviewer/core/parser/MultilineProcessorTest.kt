package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull

class MultilineProcessorTest {
    private val registry = ParserRegistry()
    private val template = registry.getTemplate("Standard")!!

    @Test
    fun `should recognize timezone-aware log as new header and flush previous`() {
        val processor = MultilineProcessor(template)
        
        processor.process("2026-05-08 00:00:00 INFO First line")
        val finishedEntry = processor.process("2026-05-08 00:27:56.321 +01:00 [INF] more stuff here")
        
        expectThat(finishedEntry).isNotNull()
        expectThat(finishedEntry!!.content.value).isEqualTo("First line")
        
        val lastEntry = processor.flush()
        expectThat(lastEntry).isNotNull()
        expectThat(lastEntry!!.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `should aggregate multiline logs`() {
        val processor = MultilineProcessor(template)
        
        // Line 1: Header
        val res1 = processor.process("2024-05-14 15:24:08 ERROR NullPointerException at")
        expectThat(res1).isNull()
        
        // Line 2: Continuation
        val res2 = processor.process("\tat com.example.Main.main(Main.kt:10)")
        expectThat(res2).isNull()
        
        // Line 3: Continuation
        val res3 = processor.process("\tat java.base/java.lang.reflect.Method.invoke(Method.java:580)")
        expectThat(res3).isNull()
        
        // Line 4: Next Header
        val res4 = processor.process("2024-05-14 15:24:09 INFO Next log")
        expectThat(res4).isNotNull()
        expectThat(res4!!.level).isEqualTo(LogLevel.ERROR)
        expectThat(res4.content.value).contains("NullPointerException")
        expectThat(res4.content.value).contains("Main.kt:10")
        
        // Flush last entry
        val res5 = processor.flush()
        expectThat(res5).isNotNull()
        expectThat(res5!!.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `should respect max buffer lines`() {
        val processor = MultilineProcessor(template, maxBufferLines = 3)
        
        processor.process("2024-05-14 15:24:08 INFO Header")
        processor.process("Continuation 1")
        processor.process("Continuation 2")
        processor.process("Continuation 3") // This should cause truncation
        
        val res = processor.flush()
        expectThat(res).isNotNull()
        expectThat(res!!.content.value).contains("Continuation 2")
        expectThat(res.content.value).contains("truncated")
        expectThat(res.content.value).not().contains("Continuation 3")
    }
}
