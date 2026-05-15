package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.time.Instant

class GapAnalysisTest {

    private val levelMapper = LevelMapper(usePrefixMatching = true)
    private val registry = ParserRegistry()

    @Test
    fun `2_1 Log Level Variations - Abbreviated Names`() {
        expectThat(levelMapper.map("INF")).isEqualTo(LogLevel.INFO)
        expectThat(levelMapper.map("WRN")).isEqualTo(LogLevel.WARN)
        expectThat(levelMapper.map("ERR")).isEqualTo(LogLevel.ERROR)
        expectThat(levelMapper.map("FTL")).isEqualTo(LogLevel.FATAL)
        expectThat(levelMapper.map("DBUG")).isEqualTo(LogLevel.DEBUG)
        expectThat(levelMapper.map("I")).isEqualTo(LogLevel.INFO)
        expectThat(levelMapper.map("W")).isEqualTo(LogLevel.WARN)
        expectThat(levelMapper.map("E")).isEqualTo(LogLevel.ERROR)
    }

    @Test
    fun `2_1 Log Level Variations - Alternative Terminology`() {
        expectThat(levelMapper.map("WARNING")).isEqualTo(LogLevel.WARN)
        expectThat(levelMapper.map("SEVERE")).isEqualTo(LogLevel.ERROR)
        expectThat(levelMapper.map("CRITICAL")).isEqualTo(LogLevel.FATAL)
        expectThat(levelMapper.map("TRACE")).isEqualTo(LogLevel.DEBUG)
        expectThat(levelMapper.map("VERBOSE")).isEqualTo(LogLevel.DEBUG)
        expectThat(levelMapper.map("NOTICE")).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun `2_1 Log Level Variations - Numeric Levels`() {
        expectThat(levelMapper.map("0")).isEqualTo(LogLevel.FATAL) // Emergency
        expectThat(levelMapper.map("3")).isEqualTo(LogLevel.ERROR) // Error
        expectThat(levelMapper.map("4")).isEqualTo(LogLevel.WARN)  // Warning
        expectThat(levelMapper.map("5")).isEqualTo(LogLevel.INFO)  // Notice
        expectThat(levelMapper.map("7")).isEqualTo(LogLevel.DEBUG) // Debug
    }

    @Test
    fun `2_1 Log Level Variations - Missing Levels`() {
        expectThat(levelMapper.map("")).isEqualTo(LogLevel.UNKNOWN)
        expectThat(levelMapper.map("XYZ")).isEqualTo(LogLevel.UNKNOWN)
    }

    @Test
    fun `2_2 Timestamp Formats - ISO8601 RFC3339`() {
        val parser = TimestampParser("yyyy-MM-dd'T'HH:mm:ssX")
        val result = parser.parse("2026-05-14T16:53:00Z")
        expectThat(result).isNotNull()
        expectThat(result).isEqualTo(Instant.parse("2026-05-14T16:53:00Z"))
    }

    @Test
    fun `2_2 Timestamp Formats - Locale-Specific Apache`() {
        val parser = TimestampParser("dd/MMM/yyyy:HH:mm:ss Z")
        val result = parser.parse("14/May/2026:16:53:00 +0000")
        expectThat(result).isNotNull()
        expectThat(result).isEqualTo(Instant.parse("2026-05-14T16:53:00Z"))
    }

    @Test
    fun `2_2 Timestamp Formats - Unix Epoch`() {
        val parser = TimestampParser("yyyy-MM-dd")
        val seconds = parser.parse("1715694780")
        expectThat(seconds).isEqualTo(Instant.ofEpochSecond(1715694780))
        
        val millis = parser.parse("1715694780000")
        expectThat(millis).isEqualTo(Instant.ofEpochMilli(1715694780000))
    }

    @Test
    fun `2_2 Timestamp Formats - Implicit Year Syslog`() {
        val parser = TimestampParser("MMM d HH:mm:ss")
        val result = parser.parse("May 14 16:53:00")
        expectThat(result).isNotNull()
        // Should use current year, which is fine for this test
    }

    @Test
    fun `2_3 Log Structure - JSON`() {
        val line = """{"ts": "2026-05-14T16:53:00Z", "level": "INFO", "msg": "Hello JSON"}"""
        val config = JsonMapping(timestampKey = "ts", levelKey = "level", contentKey = "msg")
        val configuredParser = JsonLogParser(config)
        
        val result = configuredParser.parse(line)
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Hello JSON")
        }
    }

    @Test
    fun `2_3 Log Structure - logfmt`() {
        val parser = LogfmtParser()
        val line = """time="2026-05-14 16:53:00" level=info msg="hello world" user_id=123"""
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("hello world")
        }
    }

    @Test
    fun `2_3 Log Structure - Delimited CSV`() {
        val template = registry.getTemplate("CSV")!!
        val parser = TemplateLogParser(template)
        val line = "2026-05-14,INFO,Application started"
        val result = parser.parse(line)
        
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.level).isEqualTo(LogLevel.INFO)
            expectThat(entry.content.value).isEqualTo("Application started")
        }
    }

    @Test
    fun `3_5 Auto-Detection Logic`() {
        val probe = HeuristicProbe(registry)
        
        // JSON
        expectThat(probe.detect(listOf("""{"ts": "2026-05-14", "msg": "json"}"""))).`should not be null`()
        
        // logfmt
        expectThat(probe.detect(listOf("""time=2026-05-14 level=info msg=logfmt"""))).`should not be null`()
        
        // ISO8601 Template
        expectThat(probe.detect(listOf("""2026-05-14T16:53:00Z INFO template"""))).`should not be null`()
    }

    @Test
    fun `2_4 Multiline Entries - Stack Traces`() {
        val template = registry.getTemplate("Standard")!!
        val processor = MultilineProcessor(template)
        
        processor.process("2026-05-14 16:53:00 ERROR something failed")
        processor.process("  at com.example.App.main(App.kt:10)")
        val entry = processor.flush()
        
        expectThat(entry).isNotNull()
        expectThat(entry!!.content.value).isEqualTo("something failed\n  at com.example.App.main(App.kt:10)")
    }

    @Test
    fun `2_4 Multiline Entries - JSON Fragments`() {
        val template = registry.getTemplate("Standard")!!
        val processor = MultilineProcessor(template)
        
        processor.process("2026-05-14 16:53:00 INFO Response received:")
        processor.process("{")
        processor.process("  \"status\": \"ok\",")
        processor.process("  \"code\": 200")
        processor.process("}")
        val entry = processor.flush()
        
        expectThat(entry).isNotNull()
        expectThat(entry!!.content.value).contains("\"status\": \"ok\"")
        expectThat(entry.content.value).contains("Response received:")
    }
}
