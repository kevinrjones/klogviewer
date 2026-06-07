package com.klogviewer.core.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue

class JsonConfidenceScorerTest {
    private val scorer = JsonConfidenceScorer()

    @Test
    fun `score should be higher for canonical json objects than generic objects`() {
        val canonicalObjects = listOf(
            jsonObject("""{"@timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"one"}"""),
            jsonObject("""{"@timestamp":"2024-05-14T10:00:01Z","level":"WARN","message":"two"}""")
        )
        val genericObjects = listOf(
            jsonObject("""{"a":"2024-05-14T10:00:00Z","b":"INFO","c":"one"}"""),
            jsonObject("""{"a":"2024-05-14T10:00:01Z","b":"WARN","c":"two"}""")
        )

        val canonicalScore = scorer.score(sampledCount = 2, parsedObjects = canonicalObjects, malformedCount = 0)
        val genericScore = scorer.score(sampledCount = 2, parsedObjects = genericObjects, malformedCount = 0)

        expectThat(canonicalScore.canonicalKeyHitCount).isGreaterThan(genericScore.canonicalKeyHitCount)
        expectThat(canonicalScore.finalConfidenceScore).isGreaterThan(genericScore.finalConfidenceScore)
    }

    @Test
    fun `single record without canonical aliases should stay below selection threshold`() {
        val genericSingle = listOf(
            jsonObject("""{"a":"2024-05-14T10:00:00Z","b":"INFO","c":"one"}""")
        )

        val confidence = scorer.score(sampledCount = 1, parsedObjects = genericSingle, malformedCount = 0)

        expectThat(scorer.shouldSelectJson(confidence)).isFalse()
    }

    @Test
    fun `canonical records with small malformed fraction should still pass selection threshold`() {
        val canonicalObjects = listOf(
            jsonObject("""{"timestamp":"2024-05-14T10:00:00Z","level":"INFO","message":"one"}"""),
            jsonObject("""{"timestamp":"2024-05-14T10:00:01Z","level":"WARN","message":"two"}"""),
            jsonObject("""{"timestamp":"2024-05-14T10:00:02Z","level":"ERROR","message":"three"}""")
        )

        val confidence = scorer.score(sampledCount = 4, parsedObjects = canonicalObjects, malformedCount = 1)

        expectThat(scorer.shouldSelectJson(confidence)).isTrue()
    }

    private fun jsonObject(raw: String): JsonObject = Json.parseToJsonElement(raw).jsonObject
}