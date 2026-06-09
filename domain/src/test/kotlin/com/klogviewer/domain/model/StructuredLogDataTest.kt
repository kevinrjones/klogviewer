package com.klogviewer.domain.model

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsKey
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class StructuredLogDataTest {

    @BeforeEach
    fun resetCaches() {
        resetStructuredProjectionCachesForTests()
    }

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
    fun `flattening applies array breadth limit and exposes truncation marker`() {
        val value = StructuredValue.ObjectValue(
            mapOf(
                "items" to StructuredValue.ArrayValue(
                    listOf(
                        StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("a"))),
                        StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("b"))),
                        StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("c")))
                    )
                )
            )
        )

        val flat = value.flattenToPathIndex(
            limits = StructuredFlattenLimits(maxArrayBreadth = 2)
        )

        expectThat(flat).containsKey("items[0].id")
        expectThat(flat).containsKey("items[1].id")
        expectThat(flat).containsKey("_meta.limit")
        expectThat(flat["items[].id"]).isEqualTo(
            listOf(
                StructuredValue.StringValue("a"),
                StructuredValue.StringValue("b")
            )
        )
    }

    @Test
    fun `flattening applies depth limit and exposes truncation marker`() {
        val value = StructuredValue.ObjectValue(
            mapOf(
                "a" to StructuredValue.ObjectValue(
                    mapOf(
                        "b" to StructuredValue.ObjectValue(
                            mapOf("c" to StructuredValue.StringValue("deep"))
                        )
                    )
                )
            )
        )

        val flat = value.flattenToPathIndex(
            limits = StructuredFlattenLimits(maxDepth = 1)
        )

        expectThat(flat).containsKey("_meta.limit")
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
    fun `compatibility projection includes canonical fields while preserving raw fields`() {
        val data = StructuredLogData(
            root = StructuredValue.ObjectValue(
                mapOf(
                    "@m" to StructuredValue.StringValue("rendered message"),
                    "@mt" to StructuredValue.StringValue("template {Id}"),
                    "Properties" to StructuredValue.ObjectValue(
                        mapOf("SourceContext" to StructuredValue.StringValue("OrdersController"))
                    )
                )
            ),
            canonicalFields = mapOf(
                "message" to StructuredValue.StringValue("rendered message")
            )
        )

        val projected = data.toCompatibilityFields()

        expectThat(projected["message"]).isEqualTo("rendered message")
        expectThat(projected["@m"]).isEqualTo("rendered message")
        expectThat(projected["@mt"]).isEqualTo("template {Id}")
        expectThat(projected["Properties.SourceContext"]).isEqualTo("OrdersController")
    }

    @Test
    fun `structured projection cache reuses previously projected payload`() {
        val root = StructuredValue.ObjectValue(
            mapOf(
                "service" to StructuredValue.StringValue("auth"),
                "request" to StructuredValue.ObjectValue(
                    mapOf("id" to StructuredValue.StringValue("req-1"))
                )
            )
        )

        val first = StructuredLogData(
            root = root,
            rawPayload = "{\"service\":\"auth\",\"request\":{\"id\":\"req-1\"}}",
            projectionCacheKey = "cache-key"
        )
        val second = StructuredLogData(
            root = root,
            rawPayload = "{\"service\":\"auth\",\"request\":{\"id\":\"req-1\"}}",
            projectionCacheKey = "cache-key"
        )

        expectThat(first.flatPathIndex).containsKey("request.id")
        expectThat(structuredProjectionPathCacheSizeForTests()).isEqualTo(1)
        expectThat(second.flatPathIndex).containsKey("request.id")
        expectThat(structuredProjectionPathCacheSizeForTests()).isEqualTo(1)
    }

    @Test
    fun `structured projection cache evicts oldest keys when limit is exceeded`() {
        StructuredLogData(
            root = StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("1"))),
            projectionCacheKey = "entry-1",
            cacheLimit = 2
        ).flatPathIndex

        StructuredLogData(
            root = StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("2"))),
            projectionCacheKey = "entry-2",
            cacheLimit = 2
        ).flatPathIndex

        StructuredLogData(
            root = StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("3"))),
            projectionCacheKey = "entry-3",
            cacheLimit = 2
        ).flatPathIndex

        expectThat(structuredProjectionPathCacheSizeForTests()).isEqualTo(2)
    }

    @Test
    fun `compatibility projection cache is bounded by cache limit`() {
        StructuredLogData(
            root = StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("1"))),
            projectionCacheKey = "compat-1",
            cacheLimit = 1
        ).toCompatibilityFields()

        StructuredLogData(
            root = StructuredValue.ObjectValue(mapOf("id" to StructuredValue.StringValue("2"))),
            projectionCacheKey = "compat-2",
            cacheLimit = 1
        ).toCompatibilityFields()

        expectThat(structuredProjectionCompatibilityCacheSizeForTests()).isEqualTo(1)
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
