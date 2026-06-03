package com.klogviewer.ui.components

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotEqualTo

class LogListSourceShadeIndexTest {

    @Test
    fun `given gray shade generation when requesting 50 shades then shades are produced and progressively darken`() {
        val shades = generateDarkerGrayShades(
            argb = 0xFFFAFAFA,
            count = 50,
            step = 4
        )

        expectThat(shades.size).isEqualTo(50)

        shades.zipWithNext().forEach { (previous, next) ->
            expectThat(next.red).isLessThanOrEqualTo(previous.red)
            expectThat(next.green).isLessThanOrEqualTo(previous.green)
            expectThat(next.blue).isLessThanOrEqualTo(previous.blue)
        }
    }

    @Test
    fun `given single source context when resolving source shade index then no source shade index is returned`() {
        val shadeIndex = getSourceShadeIndex("/tmp/services/api.log", listOf("/tmp/services/api.log"))

        expectThat(shadeIndex).isEqualTo(NO_SOURCE_SHADE_INDEX)
    }

    @Test
    fun `given source order changes when resolving source shade index then mapping remains stable for same source`() {
        val sourceA = "/tmp/services/api.log"
        val sourceB = "/var/log/database/db.log"

        val initialShadeIndex = getSourceShadeIndex(sourceB, listOf(sourceA, sourceB))
        val updatedShadeIndex = getSourceShadeIndex(sourceB, listOf(sourceB, "s3://prod-logs/cache/cache.log"))

        expectThat(initialShadeIndex).isEqualTo(updatedShadeIndex)
    }

    @Test
    fun `given distinct sources when resolving source shade index then deterministic shades can differ per source`() {
        val sourceBShadeIndex = getSourceShadeIndex(
            "/var/log/database/db.log",
            listOf("/var/log/database/db.log", "s3://prod-logs/cache/cache.log")
        )
        val sourceCShadeIndex = getSourceShadeIndex(
            "s3://prod-logs/cache/cache.log",
            listOf("/var/log/database/db.log", "s3://prod-logs/cache/cache.log")
        )

        expectThat(sourceBShadeIndex).isNotEqualTo(sourceCShadeIndex)
    }

    @Test
    fun `given many potential sources when resolving source shade index then generated palette supports additional shades`() {
        val observedShadeIndexes = mutableSetOf<Int>()

        var seed = 0
        while (observedShadeIndexes.size < 50 && seed < 5_000) {
            val sourceId = "generated-source-$seed"
            observedShadeIndexes += getSourceShadeIndex(sourceId, listOf("source-a", sourceId))
            seed += 1
        }

        expectThat(observedShadeIndexes.size).isEqualTo(50)
    }
}