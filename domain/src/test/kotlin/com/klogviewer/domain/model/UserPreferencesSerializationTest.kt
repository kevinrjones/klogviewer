package com.klogviewer.domain.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

class UserPreferencesSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `given WindowPreference when created then useCompactCellMode defaults to true`() {
        val window = WindowPreference(id = "test-id", filePath = "/test.log")
        expectThat(window.useCompactCellMode).isTrue()
    }

    @Test
    fun `given WindowPreference when serialized and deserialized then useCompactCellMode is preserved`() {
        val window = WindowPreference(
            id = "test-id",
            filePath = "/test.log",
            useCompactCellMode = false
        )
        val serialized = json.encodeToString(WindowPreference.serializer(), window)
        val deserialized = json.decodeFromString(WindowPreference.serializer(), serialized)
        expectThat(deserialized.useCompactCellMode).isEqualTo(false)
    }

    @Test
    fun `given WindowPreference when serialized and deserialized with default then useCompactCellMode is true`() {
        val window = WindowPreference(
            id = "test-id",
            filePath = "/test.log"
        )
        val serialized = json.encodeToString(WindowPreference.serializer(), window)
        val deserialized = json.decodeFromString(WindowPreference.serializer(), serialized)
        expectThat(deserialized.useCompactCellMode).isEqualTo(true)
    }

    @Test
    fun `given serialized WindowPreference without useCompactCellMode when deserialized then defaults to true`() {
        val jsonString = """{"id":"test-id","filePath":"/test.log"}"""
        val deserialized = json.decodeFromString(WindowPreference.serializer(), jsonString)
        expectThat(deserialized.useCompactCellMode).isTrue()
    }
}
