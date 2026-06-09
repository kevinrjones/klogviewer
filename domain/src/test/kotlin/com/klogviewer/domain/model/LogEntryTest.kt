package com.klogviewer.domain.model

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class LogEntryTest {

    @Test
    fun `given structured data when compatibility fields are requested repeatedly then projection is memoized`() {
        val entry = LogEntry(
            timestamp = LogTimestamp("2026-01-01T00:00:00Z"),
            level = LogLevel.INFO,
            content = LogContent("request complete"),
            fields = mapOf("service" to "raw-service"),
            structuredData = StructuredLogData(
                root = StructuredValue.ObjectValue(
                    mapOf(
                        "service" to StructuredValue.StringValue("structured-service"),
                        "request" to StructuredValue.ObjectValue(
                            mapOf("id" to StructuredValue.StringValue("req-42"))
                        )
                    )
                )
            )
        )

        val firstProjection = entry.compatibilityFields()
        val secondProjection = entry.compatibilityFields()

        expectThat(firstProjection["service"]).isEqualTo("raw-service")
        expectThat(firstProjection["request.id"]).isEqualTo("req-42")
        expectThat(firstProjection === secondProjection).isTrue()
    }
}
