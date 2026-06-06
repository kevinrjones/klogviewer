package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class StructuredLogDataTest {

    @Test
    fun `structured value supports all required value kinds`() {
        val stringValue: StructuredValue = StructuredValue.StringValue("hello")
        val numberValue: StructuredValue = StructuredValue.NumberValue("123.45")
        val booleanValue: StructuredValue = StructuredValue.BooleanValue(true)
        val nullValue: StructuredValue = StructuredValue.NullValue
        val objectValue: StructuredValue = StructuredValue.ObjectValue(
            fields = mapOf("name" to StructuredValue.StringValue("Ada"))
        )
        val arrayValue: StructuredValue = StructuredValue.ArrayValue(
            values = listOf(StructuredValue.StringValue("a"), StructuredValue.StringValue("b"))
        )

        expectThat(stringValue).isEqualTo(StructuredValue.StringValue("hello"))
        expectThat(numberValue).isEqualTo(StructuredValue.NumberValue("123.45"))
        expectThat(booleanValue).isEqualTo(StructuredValue.BooleanValue(true))
        expectThat(nullValue).isEqualTo(StructuredValue.NullValue)
        expectThat(objectValue).isEqualTo(
            StructuredValue.ObjectValue(mapOf("name" to StructuredValue.StringValue("Ada")))
        )
        expectThat(arrayValue).isEqualTo(
            StructuredValue.ArrayValue(
                listOf(StructuredValue.StringValue("a"), StructuredValue.StringValue("b"))
            )
        )
    }

    @Test
    fun `flattening nested object creates deterministic dot paths`() {
        val value = StructuredValue.ObjectValue(
            mapOf(
                "user" to StructuredValue.ObjectValue(
                    mapOf(
                        "id" to StructuredValue.NumberValue("123"),
                        "name" to StructuredValue.StringValue("Ada")
                    )
                )
            )
        )

        val flat = value.flattenToPathIndex()

        expectThat(flat["user.id"]).isEqualTo(listOf(StructuredValue.NumberValue("123")))
        expectThat(flat["user.name"]).isEqualTo(listOf(StructuredValue.StringValue("Ada")))
    }

    @Test
    fun `flattening arrays provides indexed and any-match paths`() {
        val value = StructuredValue.ObjectValue(
            mapOf(
                "items" to StructuredValue.ArrayValue(
                    listOf(
                        StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("a"))),
                        StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("b")))
                    )
                )
            )
        )

        val flat = value.flattenToPathIndex()

        expectThat(flat["items[0].id"]).isEqualTo(listOf(StructuredValue.StringValue("a")))
        expectThat(flat["items[1].id"]).isEqualTo(listOf(StructuredValue.StringValue("b")))
        expectThat(flat["items[].id"]).isEqualTo(
            listOf(
                StructuredValue.StringValue("a"),
                StructuredValue.StringValue("b")
            )
        )
    }

    @Test
    fun `compatibility projection exposes flattened scalar fields and deterministic null string`() {
        val data = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "service" to StructuredValue.StringValue("auth"),
                    "user" to StructuredValue.ObjectValue(
                        mapOf(
                            "id" to StructuredValue.NumberValue("42"),
                            "deleted" to StructuredValue.NullValue
                        )
                    )
                )
            ),
            rawPayload = "{\"service\":\"auth\",\"user\":{\"id\":42,\"deleted\":null}}"
        )

        val projected = data.toCompatibilityFields()

        expectThat(projected["service"]).isEqualTo("auth")
        expectThat(projected["user.id"]).isEqualTo("42")
        expectThat(projected["user.deleted"]).isEqualTo("null")
    }

    @Test
    fun `explicit fields win on collision and structured fields fill missing keys`() {
        val data = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "level" to StructuredValue.StringValue("INFO"),
                    "request" to StructuredValue.ObjectValue(
                        mapOf("id" to StructuredValue.StringValue("req-1"))
                    )
                )
            ),
            rawPayload = "{\"level\":\"INFO\",\"request\":{\"id\":\"req-1\"}}"
        )
        val entry = LogEntry(
            timestamp = LogTimestamp("2026-06-05T12:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("line"),
            fields = mapOf("level" to "WARN"),
            structuredData = data
        )

        val compatibility = entry.compatibilityFields()

        expectThat(compatibility["level"]).isEqualTo("WARN")
        expectThat(compatibility["request.id"]).isEqualTo("req-1")
    }

    @Test
    fun `structured raw payload is preserved unchanged`() {
        val raw = "{\"items\":[{\"id\":\"a\"},{\"id\":\"b\"}]}"
        val data = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "items" to StructuredValue.ArrayValue(
                        listOf(
                            StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("a"))),
                            StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("b")))
                        )
                    )
                )
            ),
            rawPayload = raw
        )

        expectThat(data.rawPayload).isEqualTo(raw)
    }

    @Test
    fun `log entry without structured data remains backward compatible`() {
        val entry = LogEntry(
            timestamp = LogTimestamp("2026-06-05T12:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("legacy"),
            fields = mapOf("level" to "INFO")
        )

        expectThat(entry.structuredData).isNull()
        expectThat(entry.fields["level"]).isEqualTo("INFO")
        expectThat(entry.compatibilityFields()["level"]).isEqualTo("INFO")
    }

    @Test
    fun `plain entry without explicit fields keeps empty compatibility map`() {
        val entry = LogEntry(
            timestamp = LogTimestamp("2026-06-05T12:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("legacy")
        )

        expectThat(entry.fields.isEmpty()).isEqualTo(true)
        expectThat(entry.compatibilityFields().isEmpty()).isEqualTo(true)
        expectThat(entry.structuredData).isNull()
        expectThat(entry.instant).isNull()
        expectThat(entry.sourceId).isNull()
    }
}
