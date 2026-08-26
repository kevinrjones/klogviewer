package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class MultilineProcessorCompatibilityTest {

    private val compiler = PatternDraftCompiler()

    @Test
    fun `should aggregate multiline stack trace lines into single entry content`() {
        val draft = PatternImporter.importPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %level %logger - %msg").draft
        val compiled = compiler.compile(draft)

        val processor = MultilineProcessor(compiled.template, maxBufferLines = 10)

        val line1 = "2026-08-26 10:00:00.000 [main] ERROR com.example.App - NullPointerException encountered"
        val line2 = "\tat com.example.App.main(App.java:42)"
        val line3 = "\tat java.base/java.lang.Thread.run(Thread.java:833)"
        val line4 = "2026-08-26 10:00:01.000 [main] INFO com.example.App - Next entry started"

        expectThat(processor.process(line1)).isNull()
        expectThat(processor.process(line2)).isNull()
        expectThat(processor.process(line3)).isNull()

        val firstEntry = processor.process(line4)
        expectThat(firstEntry).isNotNull()
        expectThat(firstEntry!!.level).isEqualTo(LogLevel.ERROR)
        expectThat(firstEntry.content.value).isEqualTo("NullPointerException encountered\n$line2\n$line3")

        val flushedEntry = processor.flush()
        expectThat(flushedEntry).isNotNull()
        expectThat(flushedEntry!!.level).isEqualTo(LogLevel.INFO)
        expectThat(flushedEntry.content.value).isEqualTo("Next entry started")
    }

    @Test
    fun `should enforce max buffer lines on oversized multiline entries`() {
        val draft = PatternImporter.importPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %level %msg").draft
        val compiled = compiler.compile(draft)

        val processor = MultilineProcessor(compiled.template, maxBufferLines = 3)

        processor.process("2026-08-26 10:00:00.000 ERROR Header line")
        processor.process("  continuation 1")
        processor.process("  continuation 2")
        processor.process("  continuation 3") // exceeds limit

        val entry = processor.flush()
        expectThat(entry).isNotNull()
        expectThat(entry!!.content.value.contains("[truncated due to buffer limit]")).isTrue()
    }

    @Test
    fun `should handle single line logs without regression`() {
        val draft = PatternImporter.importPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %level %msg").draft
        val compiled = compiler.compile(draft)

        val processor = MultilineProcessor(compiled.template)

        expectThat(processor.process("2026-08-26 10:00:00.000 INFO Line 1")).isNull()

        val entry1 = processor.process("2026-08-26 10:00:01.000 WARN Line 2")
        expectThat(entry1).isNotNull()
        expectThat(entry1!!.level).isEqualTo(LogLevel.INFO)

        val entry2 = processor.flush()
        expectThat(entry2).isNotNull()
        expectThat(entry2!!.level).isEqualTo(LogLevel.WARN)
    }
}
