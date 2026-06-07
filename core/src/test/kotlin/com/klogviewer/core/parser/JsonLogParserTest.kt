package com.klogviewer.core.parser

import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.StructuredValue
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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

    @Test
    fun `should add canonical fields while preserving raw aliases and namespaces`() {
        val parser = JsonLogParser()
        val json =
            """{"@t":"2024-05-14T10:00:00Z","@l":"WARN","@m":"rendered message","@mt":"template {UserId}","@x":"boom","@tr":"trace-123","@sp":"span-456","Properties":{"SourceContext":"OrdersController","RequestId":"req-1"},"attributes":{"http":{"method":"GET"}}}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val structuredData = entry.structuredData
            expectThat(structuredData).isNotNull()
            expectThat(structuredData?.canonicalFields?.get("timestamp"))
                .isEqualTo(StructuredValue.StringValue("2024-05-14T10:00:00Z"))
            expectThat(structuredData?.canonicalFields?.get("level"))
                .isEqualTo(StructuredValue.StringValue("WARN"))
            expectThat(structuredData?.canonicalFields?.get("message"))
                .isEqualTo(StructuredValue.StringValue("rendered message"))
            expectThat(structuredData?.canonicalFields?.get("exception"))
                .isEqualTo(StructuredValue.StringValue("boom"))
            expectThat(structuredData?.canonicalFields?.get("trace.id"))
                .isEqualTo(StructuredValue.StringValue("trace-123"))
            expectThat(structuredData?.canonicalFields?.get("span.id"))
                .isEqualTo(StructuredValue.StringValue("span-456"))

            expectThat(structuredData?.flatPathIndex?.get("@mt"))
                .isEqualTo(listOf(StructuredValue.StringValue("template {UserId}")))
            expectThat(structuredData?.flatPathIndex?.get("Properties.SourceContext"))
                .isEqualTo(listOf(StructuredValue.StringValue("OrdersController")))
            expectThat(structuredData?.flatPathIndex?.get("attributes.http.method"))
                .isEqualTo(listOf(StructuredValue.StringValue("GET")))

            val compatibility = entry.compatibilityFields()
            expectThat(compatibility["message"]).isEqualTo("rendered message")
            expectThat(compatibility["@mt"]).isEqualTo("template {UserId}")
            expectThat(compatibility["Properties.SourceContext"]).isEqualTo("OrdersController")
        }
    }

    @Test
    fun `should prefer explicit canonical fields and keep null aliases from overriding`() {
        val parser = JsonLogParser()
        val json =
            """{"timestamp":"canonical-ts","@timestamp":"alias-ts","level":"INFO","severity":null,"message":"canonical-message","@m":"rendered","@mt":"template"}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("timestamp")).isEqualTo(StructuredValue.StringValue("canonical-ts"))
            expectThat(canonical?.get("level")).isEqualTo(StructuredValue.StringValue("INFO"))
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("canonical-message"))
            expectThat(entry.content.value).isEqualTo("canonical-message")
        }
    }

    @Test
    fun `should apply deterministic alias order when canonical keys are absent`() {
        val parser = JsonLogParser()
        val json =
            """{"@timestamp":"first-ts","time":"second-ts","lvl":"WARN","@l":"ERROR","msg":"first message","@m":"second message","logger_name":"logger-a","SourceContext":"logger-b","error":"error-first","Exception":"error-second","TraceId":"trace-upper","@tr":"trace-at","SpanId":"span-upper","@sp":"span-at"}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("timestamp")).isEqualTo(StructuredValue.StringValue("first-ts"))
            expectThat(canonical?.get("level")).isEqualTo(StructuredValue.StringValue("WARN"))
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("first message"))
            expectThat(canonical?.get("logger")).isEqualTo(StructuredValue.StringValue("logger-a"))
            expectThat(canonical?.get("exception")).isEqualTo(StructuredValue.StringValue("error-first"))
            expectThat(canonical?.get("trace.id")).isEqualTo(StructuredValue.StringValue("trace-upper"))
            expectThat(canonical?.get("span.id")).isEqualTo(StructuredValue.StringValue("span-upper"))
        }
    }

    @Test
    fun `should prefer rendered message over template and keep template raw field`() {
        val parser = JsonLogParser()
        val json = """{"@t":"2024-05-14T10:00:00Z","@l":"INFO","@m":"rendered","@mt":"template"}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("rendered"))
            expectThat(entry.compatibilityFields()["@mt"]).isEqualTo("template")
        }
    }

    @Test
    fun `should fallback canonical message to template when rendered message is absent`() {
        val parser = JsonLogParser()
        val json = """{"@t":"2024-05-14T10:00:00Z","@l":"INFO","@mt":"template only"}"""

        val result = parser.parse(json)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("template only"))
            expectThat(entry.content.value).isEqualTo("template only")
        }
    }

    @Test
    fun `should normalize representative jvm and dotnet fixtures with baseline aliases`() {
        val parser = JsonLogParser()

        val logstashJson =
            """{"@timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"logstash message","logger_name":"app.logger","metadata":{"env":"prod"}}"""
        val springJson =
            """{"timestamp":"2024-05-14T10:00:01Z","level":"WARN","logger":"org.example.Service","thread":"main","message":"spring message"}"""
        val melJson =
            """{"Timestamp":"2024-05-14T10:00:02Z","LogLevel":"Error","Category":"Microsoft.Hosting.Lifetime","Message":"mel message","Exception":"failure","TraceId":"trace-1","SpanId":"span-1"}"""
        val log4j2LikeJson =
            """{"timeMillis":1715680800000,"level":"DEBUG","loggerName":"org.example.Log4j","message":"log4j2 message","thrown":"stack"}"""

        val parsed = listOf(logstashJson, springJson, melJson, log4j2LikeJson).map { json ->
            parser.parse(json)
        }

        expectThat(parsed.all { result -> result.isRight() }).isTrue()

        val entries = parsed.map { result -> result.getOrNull() }
        expectThat(entries).hasSize(4)

        val logstashCanonical = entries[0]?.structuredData?.canonicalFields
        expectThat(logstashCanonical?.get("timestamp")).isEqualTo(StructuredValue.StringValue("2024-05-14T10:00:00Z"))
        expectThat(logstashCanonical?.get("logger")).isEqualTo(StructuredValue.StringValue("app.logger"))

        val springCanonical = entries[1]?.structuredData?.canonicalFields
        expectThat(springCanonical?.get("logger"))
            .isEqualTo(StructuredValue.StringValue("org.example.Service"))

        val melCanonical = entries[2]?.structuredData?.canonicalFields
        expectThat(melCanonical?.get("timestamp")).isEqualTo(StructuredValue.StringValue("2024-05-14T10:00:02Z"))
        expectThat(melCanonical?.get("level")).isEqualTo(StructuredValue.StringValue("Error"))
        expectThat(melCanonical?.get("logger"))
            .isEqualTo(StructuredValue.StringValue("Microsoft.Hosting.Lifetime"))
        expectThat(melCanonical?.get("exception")).isEqualTo(StructuredValue.StringValue("failure"))
        expectThat(melCanonical?.get("trace.id")).isEqualTo(StructuredValue.StringValue("trace-1"))
        expectThat(melCanonical?.get("span.id")).isEqualTo(StructuredValue.StringValue("span-1"))

        val log4j2Canonical = entries[3]?.structuredData?.canonicalFields
        expectThat(log4j2Canonical?.get("timestamp")).isNull()
        expectThat(log4j2Canonical?.get("logger")).isNull()
        expectThat(log4j2Canonical?.get("message")).isEqualTo(StructuredValue.StringValue("log4j2 message"))

        val log4j2Structured = entries[3]?.structuredData
        expectThat(log4j2Structured?.flatPathIndex?.get("timeMillis"))
            .isEqualTo(listOf(StructuredValue.NumberValue("1715680800000")))
        expectThat(log4j2Structured?.flatPathIndex?.get("loggerName"))
            .isEqualTo(listOf(StructuredValue.StringValue("org.example.Log4j")))
    }

    @Test
    fun `should parse valid entries from mixed validity lines without throwing`() {
        val parser = JsonLogParser()
        val lines = listOf(
            """{"timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"first"}""",
            "this is not json",
            """{"timestamp":"2024-05-14T10:00:01Z","level":"WARN","message":"second"}"""
        )

        val parsedEntries = lines.mapNotNull { line -> parser.parse(line).getOrNull() }

        expectThat(parsedEntries).hasSize(2)
        expectThat(parsedEntries.map { it.content.value }).isEqualTo(listOf("first", "second"))
    }

    @Test
    fun `should return no parsed entries for all invalid lines`() {
        val parser = JsonLogParser()
        val lines = listOf("not json", "still not json", "{missing:quote}")

        val parsedEntries = lines.mapNotNull { line -> parser.parse(line).getOrNull() }

        expectThat(parsedEntries).isEmpty()
    }
}
