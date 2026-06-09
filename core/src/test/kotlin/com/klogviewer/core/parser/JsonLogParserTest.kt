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
        expectThat(log4j2Canonical?.get("timestamp")).isEqualTo(StructuredValue.NumberValue("1715680800000"))
        expectThat(log4j2Canonical?.get("logger")).isEqualTo(StructuredValue.StringValue("org.example.Log4j"))
        expectThat(log4j2Canonical?.get("message")).isEqualTo(StructuredValue.StringValue("log4j2 message"))

        val log4j2Structured = entries[3]?.structuredData
        expectThat(log4j2Structured?.flatPathIndex?.get("timeMillis"))
            .isEqualTo(listOf(StructuredValue.NumberValue("1715680800000")))
        expectThat(log4j2Structured?.flatPathIndex?.get("loggerName"))
            .isEqualTo(listOf(StructuredValue.StringValue("org.example.Log4j")))
    }

    @Test
    fun `should normalize sprint 12d jvm and dotnet fixtures`() {
        val parser = JsonLogParser()
        val fixtures = sprint12dJvmAndDotnetFixtureExpectations()

        fixtures.forEach { fixture ->
            val result = parser.parse(fixture.json)
            expectThat(result.isRight()).isTrue()

            result.map { entry ->
                val canonical = entry.structuredData?.canonicalFields
                fixture.expectedCanonicalFields.forEach { (field, expectedValue) ->
                    expectThat(canonical?.get(field)).isEqualTo(expectedValue)
                }

                val flatPathIndex = entry.structuredData?.flatPathIndex
                fixture.expectedRawPaths.forEach { (path, expectedValues) ->
                    expectThat(flatPathIndex?.get(path)).isEqualTo(expectedValues)
                }
            }
        }
    }

    private fun sprint12dJvmAndDotnetFixtureExpectations(): List<FixtureExpectation> {
        return listOf(
            logstashFixture(),
            logbackMdcFixture(),
            springBootFixture(),
            log4jLayoutFixture(),
            log4jTemplateFixture(),
            melFixture(),
            serilogCompactFixture(),
            serilogRenderedCompactFixture(),
            serilogStandardFixture(),
            serilogAspNetFixture(),
            nlogFixture(),
            log4netFixture()
        )
    }

    private fun logstashFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.LOGSTASH_LOGBACK_JSON,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.StringValue("2026-06-01T10:00:00Z"),
                "level" to StructuredValue.StringValue("INFO"),
                "message" to StructuredValue.StringValue("logstash handled request"),
                "logger" to StructuredValue.StringValue("com.example.Orders")
            ),
            expectedRawPaths = mapOf(
                "mdc.traceId" to listOf(StructuredValue.StringValue("trace-jvm-1")),
                "payload.orderId" to listOf(StructuredValue.StringValue("o-123"))
            )
        )
    }

    private fun logbackMdcFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.LOGBACK_JSON_WITH_MDC,
            expectedCanonicalFields = mapOf(
                "trace.id" to StructuredValue.StringValue("trace-jvm-2"),
                "span.id" to StructuredValue.StringValue("span-jvm-2")
            ),
            expectedRawPaths = mapOf(
                "MDC.tenant" to listOf(StructuredValue.StringValue("blue")),
                "MDC.requestId" to listOf(StructuredValue.StringValue("req-jvm-2"))
            )
        )
    }

    private fun springBootFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.SPRING_BOOT_STRUCTURED_JSON,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.StringValue("2026-06-01T10:00:02Z"),
                "level" to StructuredValue.StringValue("ERROR"),
                "logger" to StructuredValue.StringValue("org.springframework.web"),
                "exception" to StructuredValue.StringValue("IllegalStateException"),
                "trace.id" to StructuredValue.StringValue("trace-spring-1"),
                "span.id" to StructuredValue.StringValue("span-spring-1")
            )
        )
    }

    private fun log4jLayoutFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.LOG4J2_JSON_LAYOUT,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.NumberValue("1780308003000"),
                "logger" to StructuredValue.StringValue("org.example.log4j.Layout"),
                "exception" to StructuredValue.StringValue("java.lang.RuntimeException: boom"),
                "trace.id" to StructuredValue.StringValue("trace-log4j-1"),
                "span.id" to StructuredValue.StringValue("span-log4j-1")
            )
        )
    }

    private fun log4jTemplateFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.LOG4J2_JSON_TEMPLATE_LAYOUT,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.StringValue("2026-06-01T10:00:04Z"),
                "level" to StructuredValue.StringValue("INFO"),
                "logger" to StructuredValue.StringValue("org.example.log4j.Template"),
                "message" to StructuredValue.StringValue("template layout event"),
                "trace.id" to StructuredValue.StringValue("trace-log4j-2"),
                "span.id" to StructuredValue.StringValue("span-log4j-2")
            )
        )
    }

    private fun melFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.MEL_JSON_CONSOLE,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.StringValue("2026-06-01T10:01:00Z"),
                "level" to StructuredValue.StringValue("Information"),
                "logger" to StructuredValue.StringValue("Microsoft.Hosting.Lifetime"),
                "message" to StructuredValue.StringValue("MEL application started"),
                "trace.id" to StructuredValue.StringValue("trace-mel-1"),
                "span.id" to StructuredValue.StringValue("span-mel-1"),
                "correlation.id" to StructuredValue.StringValue("req-mel-1")
            ),
            expectedRawPaths = mapOf(
                "EventId.Id" to listOf(StructuredValue.NumberValue("42")),
                "Scopes[0].RequestId" to listOf(StructuredValue.StringValue("req-mel-1"))
            )
        )
    }

    private fun serilogCompactFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.SERILOG_COMPACT_JSON,
            expectedCanonicalFields = mapOf(
                "timestamp" to StructuredValue.StringValue("2026-06-01T10:01:01Z"),
                "level" to StructuredValue.StringValue("Error"),
                "message" to StructuredValue.StringValue("Order {OrderId} failed for {UserId}"),
                "message.template" to StructuredValue.StringValue("Order {OrderId} failed for {UserId}"),
                "exception" to StructuredValue.StringValue("System.Exception: boom"),
                "trace.id" to StructuredValue.StringValue("trace-seri-1"),
                "span.id" to StructuredValue.StringValue("span-seri-1")
            ),
            expectedRawPaths = mapOf(
                "Properties.SourceContext" to listOf(StructuredValue.StringValue("OrdersController")),
                "Properties.RequestId" to listOf(StructuredValue.StringValue("req-seri-1"))
            )
        )
    }

    private fun serilogRenderedCompactFixture(): FixtureExpectation {
        val renderedTemplate =
            "HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000} ms"
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.SERILOG_RENDERED_COMPACT_JSON,
            expectedCanonicalFields = mapOf(
                "message" to StructuredValue.StringValue("HTTP GET /orders responded 500 in 123.45 ms"),
                "message.template" to StructuredValue.StringValue(renderedTemplate),
                "trace.id" to StructuredValue.StringValue("trace-seri-2"),
                "span.id" to StructuredValue.StringValue("span-seri-2"),
                "correlation.id" to StructuredValue.StringValue("req-seri-2")
            ),
            expectedRawPaths = mapOf(
                "RequestPath" to listOf(StructuredValue.StringValue("/orders")),
                "StatusCode" to listOf(StructuredValue.NumberValue("500"))
            )
        )
    }

    private fun serilogStandardFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.SERILOG_STANDARD_JSON,
            expectedCanonicalFields = mapOf(
                "message" to StructuredValue.StringValue("Queue payments delayed"),
                "message.template" to StructuredValue.StringValue("Queue {QueueName} delayed")
            ),
            expectedRawPaths = mapOf(
                "Properties.QueueName" to listOf(StructuredValue.StringValue("payments"))
            )
        )
    }

    private fun serilogAspNetFixture(): FixtureExpectation {
        val renderedTemplate =
            "HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000} ms"
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.SERILOG_ASPNET_REQUEST_JSON,
            expectedCanonicalFields = mapOf(
                "message" to StructuredValue.StringValue("HTTP POST /checkout responded 201 in 55.20 ms"),
                "message.template" to StructuredValue.StringValue(renderedTemplate),
                "trace.id" to StructuredValue.StringValue("trace-aspnet-1"),
                "span.id" to StructuredValue.StringValue("span-aspnet-1"),
                "correlation.id" to StructuredValue.StringValue("req-aspnet-1")
            ),
            expectedRawPaths = mapOf(
                "RequestMethod" to listOf(StructuredValue.StringValue("POST")),
                "Elapsed" to listOf(StructuredValue.NumberValue("55.2"))
            )
        )
    }

    private fun nlogFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.NLOG_JSON_LAYOUT,
            expectedCanonicalFields = mapOf(
                "message" to StructuredValue.StringValue("nlog warning"),
                "logger" to StructuredValue.StringValue("NLogLogger")
            ),
            expectedRawPaths = mapOf(
                "properties.requestId" to listOf(StructuredValue.StringValue("req-nlog-1")),
                "properties.traceId" to listOf(StructuredValue.StringValue("trace-nlog-1"))
            )
        )
    }

    private fun log4netFixture(): FixtureExpectation {
        return FixtureExpectation(
            json = StructuredEcosystemFixtures.LOG4NET_JSON_STYLE,
            expectedCanonicalFields = mapOf(
                "level" to StructuredValue.StringValue("ERROR"),
                "message" to StructuredValue.StringValue("log4net failed")
            ),
            expectedRawPaths = mapOf(
                "properties.correlationId" to listOf(StructuredValue.StringValue("corr-log4net-1"))
            )
        )
    }

    @Test
    fun `should canonicalize nested wrapper payloads and preserve envelope metadata`() {
        val parser = JsonLogParser()

        val dockerResult = parser.parse(StructuredEcosystemFixtures.DOCKER_JSON_WRAPPER)
        val kubernetesResult = parser.parse(StructuredEcosystemFixtures.KUBERNETES_CRI_WRAPPER)
        val cloudResult = parser.parse(StructuredEcosystemFixtures.CLOUD_PROVIDER_ENVELOPE)
        val otelResult = parser.parse(StructuredEcosystemFixtures.OTEL_LIKE_JSON)

        expectThat(listOf(dockerResult, kubernetesResult, cloudResult, otelResult).all { it.isRight() }).isTrue()

        dockerResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("docker app ready"))
            expectThat(canonical?.get("trace.id")).isEqualTo(StructuredValue.StringValue("trace-docker-1"))
            expectThat(canonical?.get("span.id")).isEqualTo(StructuredValue.StringValue("span-docker-1"))
            expectThat(canonical?.get("correlation.id")).isEqualTo(StructuredValue.StringValue("req-docker-1"))
            expectThat(entry.structuredData?.flatPathIndex?.get("stream"))
                .isEqualTo(listOf(StructuredValue.StringValue("stdout")))
            expectThat(entry.structuredData?.flatPathIndex?.get("_decoded.log.message"))
                .isEqualTo(listOf(StructuredValue.StringValue("docker app ready")))
        }

        kubernetesResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("k8s app failed"))
            expectThat(canonical?.get("logger")).isEqualTo(StructuredValue.StringValue("com.example.K8s"))
            expectThat(canonical?.get("correlation.id")).isEqualTo(StructuredValue.StringValue("req-k8s-1"))
            expectThat(entry.structuredData?.flatPathIndex?.get("kubernetes.namespace"))
                .isEqualTo(listOf(StructuredValue.StringValue("payments")))
            expectThat(entry.structuredData?.flatPathIndex?.get("_decoded.log.traceId"))
                .isEqualTo(listOf(StructuredValue.StringValue("trace-k8s-1")))
        }

        cloudResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("cloud nested app warning"))
            expectThat(canonical?.get("logger")).isEqualTo(StructuredValue.StringValue("com.example.Cloud"))
            expectThat(canonical?.get("trace.id")).isEqualTo(StructuredValue.StringValue("trace-cloud-1"))
            expectThat(canonical?.get("span.id")).isEqualTo(StructuredValue.StringValue("span-cloud-1"))
            expectThat(entry.structuredData?.flatPathIndex?.get("resource.type"))
                .isEqualTo(listOf(StructuredValue.StringValue("cloud_run_revision")))
            expectThat(entry.structuredData?.flatPathIndex?.get("jsonPayload.message"))
                .isEqualTo(listOf(StructuredValue.StringValue("cloud nested app warning")))
        }

        otelResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("timestamp")).isEqualTo(StructuredValue.StringValue("1780308003000123456"))
            expectThat(canonical?.get("level")).isEqualTo(StructuredValue.StringValue("ERROR"))
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("otel export failed"))
            expectThat(entry.structuredData?.flatPathIndex?.get("resource.service\\.name"))
                .isEqualTo(listOf(StructuredValue.StringValue("checkout")))
            expectThat(entry.structuredData?.flatPathIndex?.get("attributes.RequestId"))
                .isEqualTo(listOf(StructuredValue.StringValue("req-otel-1")))
        }
    }

    @Test
    fun `should preserve raw decoded field when derived decoded namespace collides`() {
        val parser = JsonLogParser()
        val wrapperWithDecodedCollision =
            """{"timestamp":"2026-06-01T13:00:00Z","level":"INFO","_decoded":{"existing":"raw-value"},"log":"{\"message\":\"nested collision payload\",\"traceId\":\"trace-collision-1\"}"}"""

        val result = parser.parse(wrapperWithDecodedCollision)

        expectThat(result.isRight()).isTrue()
        result.map { entry ->
            expectThat(entry.structuredData?.canonicalFields?.get("message"))
                .isEqualTo(StructuredValue.StringValue("nested collision payload"))
            expectThat(entry.structuredData?.canonicalFields?.get("trace.id"))
                .isEqualTo(StructuredValue.StringValue("trace-collision-1"))
            expectThat(entry.structuredData?.flatPathIndex?.get("_decoded.existing"))
                .isEqualTo(listOf(StructuredValue.StringValue("raw-value")))
            expectThat(entry.structuredData?.flatPathIndex?.get("_decoded_derived.log.message"))
                .isEqualTo(listOf(StructuredValue.StringValue("nested collision payload")))
        }
    }

    @Test
    fun `should enforce rendered message precedence and template fallback deterministically`() {
        val parser = JsonLogParser()
        val melTemplateOnly =
            """{"Timestamp":"2026-06-01T12:00:00Z","LogLevel":"Warning","OriginalFormat":"MEL template only {OrderId}"}"""

        val renderedResult = parser.parse(StructuredEcosystemFixtures.SERILOG_STANDARD_JSON)
        val templateFallbackResult = parser.parse(melTemplateOnly)

        expectThat(renderedResult.isRight()).isTrue()
        expectThat(templateFallbackResult.isRight()).isTrue()

        renderedResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("Queue payments delayed"))
            expectThat(canonical?.get("message.template"))
                .isEqualTo(StructuredValue.StringValue("Queue {QueueName} delayed"))
        }

        templateFallbackResult.map { entry ->
            val canonical = entry.structuredData?.canonicalFields
            expectThat(canonical?.get("message")).isEqualTo(StructuredValue.StringValue("MEL template only {OrderId}"))
            expectThat(canonical?.get("message.template"))
                .isEqualTo(StructuredValue.StringValue("MEL template only {OrderId}"))
        }
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

    private data class FixtureExpectation(
        val json: String,
        val expectedCanonicalFields: Map<String, StructuredValue>,
        val expectedRawPaths: Map<String, List<StructuredValue>> = emptyMap()
    )
}
