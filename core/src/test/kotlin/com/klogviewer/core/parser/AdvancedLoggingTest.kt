package com.klogviewer.core.parser

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class AdvancedLoggingTest {

    private val registry = ParserRegistry()

    @Test
    fun `should parse Syslog with hostname and process`() {
        val template = registry.getTemplate("Syslog")!!
        val parser = TemplateLogParser(template)
        val line = "May 15 00:00:06 Kevins-MacBook-Pro syslogd[358]: ASL Sender Statistics"
        
        val result = parser.parse(line)
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.fields["hostname"]).isEqualTo("Kevins-MacBook-Pro")
            expectThat(entry.fields["process"]).isEqualTo("syslogd")
            expectThat(entry.fields["pid"]).isEqualTo("358")
            expectThat(entry.content.value).isEqualTo("ASL Sender Statistics")
        }
    }

    @Test
    fun `should parse Apache Combined log`() {
        val template = registry.getTemplate("Apache")!!
        val parser = TemplateLogParser(template)
        val line = """192.168.1.10 - - [15/May/2026:10:23:42 +0000] "GET /index.html HTTP/1.1" 200 1024 "http://referrer.com" "Mozilla/5.0""""
        
        val result = parser.parse(line)
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.fields["clientIp"]).isEqualTo("192.168.1.10")
            expectThat(entry.fields["user"]).isEqualTo("-")
            expectThat(entry.fields["request"]).isEqualTo("GET /index.html HTTP/1.1")
            expectThat(entry.fields["status"]).isEqualTo("200")
            expectThat(entry.fields["bytes"]).isEqualTo("1024")
            expectThat(entry.fields["referrer"]).isEqualTo("http://referrer.com")
            expectThat(entry.fields["agent"]).isEqualTo("Mozilla/5.0")
        }
    }

    @Test
    fun `should parse Nginx log (compatible with Apache template)`() {
        val template = registry.getTemplate("Apache")!!
        val parser = TemplateLogParser(template)
        val line = """192.168.1.10 - - [15/May/2026:10:23:42 +0000] "GET / HTTP/1.1" 200 612 "-" "Mozilla/5.0""""
        
        val result = parser.parse(line)
        expectThat(result.isRight()).isTrue()
        result.onRight { entry ->
            expectThat(entry.fields["clientIp"]).isEqualTo("192.168.1.10")
            expectThat(entry.fields["status"]).isEqualTo("200")
            expectThat(entry.fields["bytes"]).isEqualTo("612")
        }
    }
}
