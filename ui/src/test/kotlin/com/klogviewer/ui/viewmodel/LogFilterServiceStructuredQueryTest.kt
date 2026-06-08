package com.klogviewer.ui.viewmodel

import com.klogviewer.domain.model.LogContent
import com.klogviewer.domain.model.LogEntry
import com.klogviewer.domain.model.LogLevel
import com.klogviewer.domain.model.LogTimestamp
import com.klogviewer.domain.model.StructuredLogData
import com.klogviewer.domain.model.StructuredValue
import com.klogviewer.ui.mvi.LogWindow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import java.time.Instant

class LogFilterServiceStructuredQueryTest {

    @Test
    fun `structured predicates evaluate exact contains regex numeric boolean exists missing and null`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "field:Properties.UserId=\"u-123\"")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:StatusCode >= 500")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:TraceId exists")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:TraceId missing")).containsExactly("all healthy")
        expectThat(filter(logs, "has:trace.id")).containsExactly("timeout happened")
        expectThat(filter(logs, "message contains \"timeout\"")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:message ~ \"timeout|deadline\"")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:durationMs > 250")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:isRetry = true")).containsExactly("timeout happened")
        expectThat(filter(logs, "field:error = null")).containsExactly("timeout happened")
        expectThat(filter(logs, "level:error")).containsExactly("timeout happened")
    }

    @Test
    fun `boolean composition supports or and parenthesized precedence`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry(),
            infoEntry()
        )

        expectThat(filter(logs, "level:error OR level:warn")).containsExactly(
            "timeout happened",
            "all healthy"
        )

        expectThat(filter(logs, "(level:error OR level:warn) AND message contains \"timeout\"")).containsExactly(
            "timeout happened"
        )
    }

    @Test
    fun `plain text filters still match content and timestamp`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "timeout")).containsExactly("timeout happened")
        expectThat(filter(logs, "2026-01-01")).containsExactly("timeout happened", "all healthy")
    }

    @Test
    fun `multiple filter chips remain and composed`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry()
        )

        val filtered = LogFilterService.filter(
            LogWindow(
                id = "window-1",
                logs = logs,
                filterQueries = listOf(
                    "message contains \"timeout\"",
                    "field:StatusCode >= 500"
                )
            )
        )

        expectThat(filtered).hasSize(1)
        expectThat(filtered.map { it.content.value }).containsExactly("timeout happened")
    }

    @Test
    fun `legacy dashboard field query remains supported`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "@field:service=auth")).containsExactly("timeout happened")
    }

    @Test
    fun `malformed structured query falls back safely to text matching`() = runTest {
        val logs = listOf(
            matchingEntry(content = "field:TraceId >= happened"),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "field:TraceId >=")).containsExactly("field:TraceId >= happened")
    }

    @Test
    fun `invalid regex does not crash and safely evaluates to non match`() = runTest {
        val logs = listOf(
            matchingEntry(),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "field:message ~ \"[\"")).hasSize(0)
    }

    @Test
    fun `explicit field paths remain precise while canonical aliases fan out`() = runTest {
        val logs = listOf(
            rawTraceIdEntry(),
            canonicalTraceIdEntry(),
            atTraceIdEntry(),
            camelTraceIdEntry()
        )

        expectThat(filter(logs, "field:TraceId=\"abc\"")).containsExactly("raw-trace-id")
        expectThat(filter(logs, "field:@tr=\"abc\"")).containsExactly("at-trace-id")
        expectThat(filter(logs, "field:traceId=\"abc\"")).containsExactly("camel-trace-id")
        expectThat(filter(logs, "field:trace.id=\"abc\"")).containsExactly("canonical-trace-id")
        expectThat(filter(logs, "trace.id = \"abc\""))
            .containsExactly("raw-trace-id", "canonical-trace-id", "at-trace-id", "camel-trace-id")
    }

    @Test
    fun `array predicates use any match semantics by default`() = runTest {
        val logs = listOf(
            arrayMatchEntry(),
            arrayWithoutIdEntry()
        )

        expectThat(filter(logs, "field:items.id=\"b2\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.status contains \"fail\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.id ~ \"a1|b2\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.durationMs > 100")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.isRetry = true")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.id exists")).containsExactly("array-match")
        expectThat(filter(logs, "field:items.id missing")).containsExactly("array-no-id")
        expectThat(filter(logs, "field:StatusCode >= 500")).containsExactly("array-match")
    }

    @Test
    fun `indexed paths target specific array elements and out of range behaves as missing`() = runTest {
        val logs = listOf(
            arrayMatchEntry(),
            arrayWithoutIdEntry()
        )

        expectThat(filter(logs, "field:items[0].id=\"a1\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:items[1].id=\"b2\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:items[1].durationMs > 100")).containsExactly("array-match")
        expectThat(filter(logs, "field:items[2].id exists")).hasSize(0)
        expectThat(filter(logs, "field:items[2].id missing")).containsExactly("array-match", "array-no-id")
        expectThat(filter(logs, "field:items[0].id missing")).containsExactly("array-no-id")
        expectThat(filter(logs, "field:items[0].`id.with.dot`=\"literal-1\"")).containsExactly("array-match")
        expectThat(filter(logs, "field:`items.with.dot`[0].id=\"dot-1\"")).containsExactly("array-match")
    }

    @Test
    fun `malformed escaped and indexed paths fall back safely to text matching`() = runTest {
        val logs = listOf(
            matchingEntry(content = "field:items[]=1 happened"),
            nonMatchingEntry()
        )

        expectThat(filter(logs, "field:items[]=1")).containsExactly("field:items[]=1 happened")
        expectThat(filter(logs, "field:`items.id=1")).hasSize(0)
    }

    @Test
    fun `thread name field predicates support equals variants hyphen values case variants eq alias and generated queries`() = runTest {
        val logs = listOf(
            threadNameEntry(),
            threadNameNonMatchEntry()
        )

        expectThat(filter(logs, "field:Thread_name=\"eventLoopGroupProxy-4-4\"")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:Thread_name=eventLoopGroupProxy-4-4")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:Thread_name = \"eventLoopGroupProxy-4-4\"")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:Thread_name = eventLoopGroupProxy-4-4")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:thread_name=\"eventLoopGroupProxy-4-4\"")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:thread_name = eventLoopGroupProxy-4-4")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:Thread_name eq \"eventLoopGroupProxy-4-4\"")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "field:thread_name eq eventLoopGroupProxy-4-4")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, "@field:Thread_name=eventLoopGroupProxy-4-4")).containsExactly(
            "structured-thread-match"
        )
        expectThat(filter(logs, StructuredInspectorFilterFormatter.fieldPredicate("Thread_name"))).containsExactly(
            "structured-thread-match",
            "structured-thread-non-match"
        )
        expectThat(
            filter(
                logs,
                StructuredInspectorFilterFormatter.valuePredicate(
                    path = "Thread_name",
                    value = StructuredValue.StringValue("eventLoopGroupProxy-4-4")
                )
            )
        ).containsExactly("structured-thread-match")

        expectThat(filter(logs, "field:Thread_name=\"eventLoopGroupProxy-9-9\"")).hasSize(0)
    }

    @Test
    fun `thread name field predicates support escaped quoted values`() = runTest {
        val logs = listOf(
            threadNameEntry(value = "event\"Loop\\Proxy-4-4"),
            threadNameNonMatchEntry()
        )

        expectThat(filter(logs, "field:Thread_name=\"event\\\"Loop\\\\Proxy-4-4\"")).containsExactly(
            "structured-thread-match"
        )
    }

    @Test
    fun `field predicates support case variants for content timestamp and at timestamp`() = runTest {
        val logs = listOf(
            timestampCaseEntry(),
            timestampCaseNonMatchEntry()
        )

        expectThat(filter(logs, "field:content contains alpha")).containsExactly("timestamp-case-match")
        expectThat(filter(logs, "field:Content contains alpha")).containsExactly("timestamp-case-match")
        expectThat(
            filter(
                logs,
                "field:timestamp=\"2026-06-01T11:05:17.942848+01:00\""
            )
        ).containsExactly("timestamp-case-match")
        expectThat(
            filter(
                logs,
                "field:Timestamp=\"2026-06-01T11:05:17.942848+01:00\""
            )
        ).containsExactly("timestamp-case-match")
        expectThat(
            filter(
                logs,
                "field:@timestamp=\"2026-06-01T11:05:17.942848+01:00\""
            )
        ).containsExactly("timestamp-case-match")
        expectThat(
            filter(
                logs,
                "field:@Timestamp=\"2026-06-01T11:05:17.942848+01:00\""
            )
        ).containsExactly("timestamp-case-match")
    }

    private suspend fun filter(logs: List<LogEntry>, query: String): List<String> {
        val filtered = LogFilterService.filter(
            LogWindow(
                id = "window-1",
                logs = logs,
                filterQueries = listOf(query)
            )
        )

        return filtered.map { entry -> entry.content.value }
    }

    private fun matchingEntry(content: String = "timeout happened"): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp("2026-01-01T10:00:00Z"),
            level = LogLevel.ERROR,
            content = LogContent(content),
            fields = mapOf(
                "TraceId" to "abc123",
                "service" to "auth"
            ),
            instant = Instant.parse("2026-01-01T10:00:00Z"),
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "Properties" to StructuredValue.ObjectValue(
                            fields = mapOf(
                                "UserId" to StructuredValue.StringValue("u-123")
                            )
                        ),
                        "StatusCode" to StructuredValue.NumberValue("503"),
                        "durationMs" to StructuredValue.NumberValue("300"),
                        "isRetry" to StructuredValue.BooleanValue(true),
                        "error" to StructuredValue.NullValue,
                        "message" to StructuredValue.StringValue("timeout deadline reached")
                    )
                ),
                canonicalFields = mapOf(
                    "trace.id" to StructuredValue.StringValue("abc123")
                )
            )
        )
    }

    private fun nonMatchingEntry(): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp("2026-01-01T10:01:00Z"),
            level = LogLevel.WARN,
            content = LogContent("all healthy"),
            fields = mapOf(
                "service" to "billing"
            ),
            instant = Instant.parse("2026-01-01T10:01:00Z"),
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "Properties" to StructuredValue.ObjectValue(
                            fields = mapOf(
                                "UserId" to StructuredValue.StringValue("u-456")
                            )
                        ),
                        "StatusCode" to StructuredValue.NumberValue("200"),
                        "durationMs" to StructuredValue.NumberValue("120"),
                        "isRetry" to StructuredValue.BooleanValue(false),
                        "message" to StructuredValue.StringValue("startup complete")
                    )
                )
            )
        )
    }

    private fun infoEntry(): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp("2026-01-01T10:02:00Z"),
            level = LogLevel.INFO,
            content = LogContent("timeout info"),
            instant = Instant.parse("2026-01-01T10:02:00Z")
        )
    }

    private fun rawTraceIdEntry(): LogEntry {
        return baseEntry(
            content = "raw-trace-id",
            fields = mapOf("TraceId" to "abc")
        )
    }

    private fun canonicalTraceIdEntry(): LogEntry {
        return baseEntry(
            content = "canonical-trace-id",
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(fields = emptyMap()),
                canonicalFields = mapOf("trace.id" to StructuredValue.StringValue("abc"))
            )
        )
    }

    private fun atTraceIdEntry(): LogEntry {
        return baseEntry(
            content = "at-trace-id",
            fields = mapOf("@tr" to "abc")
        )
    }

    private fun camelTraceIdEntry(): LogEntry {
        return baseEntry(
            content = "camel-trace-id",
            fields = mapOf("traceId" to "abc")
        )
    }

    private fun arrayMatchEntry(): LogEntry {
        return baseEntry(
            content = "array-match",
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "StatusCode" to StructuredValue.NumberValue("503"),
                        "items" to StructuredValue.ArrayValue(
                            values = listOf(
                                StructuredValue.ObjectValue(
                                    fields = mapOf(
                                        "id" to StructuredValue.StringValue("a1"),
                                        "id.with.dot" to StructuredValue.StringValue("literal-1"),
                                        "status" to StructuredValue.StringValue("ok"),
                                        "durationMs" to StructuredValue.NumberValue("50"),
                                        "isRetry" to StructuredValue.BooleanValue(false)
                                    )
                                ),
                                StructuredValue.ObjectValue(
                                    fields = mapOf(
                                        "id" to StructuredValue.StringValue("b2"),
                                        "status" to StructuredValue.StringValue("failed upstream"),
                                        "durationMs" to StructuredValue.NumberValue("150"),
                                        "isRetry" to StructuredValue.BooleanValue(true)
                                    )
                                )
                            )
                        ),
                        "items.with.dot" to StructuredValue.ArrayValue(
                            values = listOf(
                                StructuredValue.ObjectValue(
                                    fields = mapOf(
                                        "id" to StructuredValue.StringValue("dot-1")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun arrayWithoutIdEntry(): LogEntry {
        return baseEntry(
            content = "array-no-id",
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "items" to StructuredValue.ArrayValue(
                            values = listOf(
                                StructuredValue.ObjectValue(
                                    fields = mapOf(
                                        "status" to StructuredValue.StringValue("ok"),
                                        "durationMs" to StructuredValue.NumberValue("20"),
                                        "isRetry" to StructuredValue.BooleanValue(false)
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    private fun threadNameEntry(value: String = "eventLoopGroupProxy-4-4"): LogEntry {
        return baseEntry(
            content = "structured-thread-match",
            fields = mapOf("Thread_name" to value),
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "Thread_name" to StructuredValue.StringValue(value),
                        "message" to StructuredValue.StringValue("started")
                    )
                )
            )
        )
    }

    private fun threadNameNonMatchEntry(): LogEntry {
        return baseEntry(
            content = "structured-thread-non-match",
            fields = mapOf("Thread_name" to "eventLoopGroupProxy-1-1"),
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    fields = mapOf(
                        "Thread_name" to StructuredValue.StringValue("eventLoopGroupProxy-1-1"),
                        "message" to StructuredValue.StringValue("started")
                    )
                )
            )
        )
    }

    private fun timestampCaseEntry(): LogEntry {
        return baseEntry(
            content = "timestamp-case-match",
            fields = mapOf(
                "content" to "alpha event",
                "timestamp" to "2026-06-01T11:05:17.942848+01:00",
                "@timestamp" to "2026-06-01T11:05:17.942848+01:00"
            )
        )
    }

    private fun timestampCaseNonMatchEntry(): LogEntry {
        return baseEntry(
            content = "timestamp-case-non-match",
            fields = mapOf(
                "content" to "beta event",
                "timestamp" to "2026-06-01T11:05:18.942848+01:00",
                "@timestamp" to "2026-06-01T11:05:18.942848+01:00"
            )
        )
    }

    private fun baseEntry(
        content: String,
        fields: Map<String, String> = emptyMap(),
        structuredData: StructuredLogData? = null
    ): LogEntry {
        return LogEntry(
            timestamp = LogTimestamp("2026-01-01T11:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent(content),
            fields = fields,
            instant = Instant.parse("2026-01-01T11:00:00Z"),
            structuredData = structuredData
        )
    }
}