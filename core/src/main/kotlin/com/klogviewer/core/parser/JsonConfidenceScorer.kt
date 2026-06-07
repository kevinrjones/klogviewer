package com.klogviewer.core.parser

import kotlinx.serialization.json.JsonObject
import kotlin.math.max
import kotlin.math.min

/**
 * A class responsible for scoring the confidence level of JSON data parsing
 * based on the sampled input data, parsing success rates, and malformed data ratio.
 *
 * @property confidenceThreshold The minimum confidence score required for JSON parsing to be selected.
 * @property lowSampleSingleRecordPenalty Penalty applied when there's only a single record without canonical keys.
 * @property lowSampleDoubleRecordNoCanonicalPenalty Penalty applied when there are two records without canonical keys.
 * @property lowSampleDoubleRecordCanonicalPenalty Penalty applied when there are two records with canonical keys.
 */
class JsonConfidenceScorer(
    private val confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD,
    private val lowSampleSingleRecordPenalty: Double = DEFAULT_LOW_SAMPLE_SINGLE_RECORD_PENALTY,
    private val lowSampleDoubleRecordNoCanonicalPenalty: Double = DEFAULT_LOW_SAMPLE_DOUBLE_RECORD_NO_CANONICAL_PENALTY,
    private val lowSampleDoubleRecordCanonicalPenalty: Double = DEFAULT_LOW_SAMPLE_DOUBLE_RECORD_CANONICAL_PENALTY
) {
    /**
     * Evaluates the confidence level of a JSON parsing operation.
     *
     * @param sampledCount The total number of JSON records sampled for parsing.
     * @param parsedObjects A list of successfully parsed JSON objects.
     * @param malformedCount The number of JSON records identified as malformed or invalid.
     * @return A `ParseDetectionConfidence` object representing the confidence score
     *   and related evaluation metrics for the parsing operation.
     */
    fun score(
        sampledCount: Int,
        parsedObjects: List<JsonObject>,
        malformedCount: Int
    ): ParseDetectionConfidence {
        val successfulParseCount = parsedObjects.size
        val parseSuccessRatio = successfulParseCount.toRatio(sampledCount)
        val malformedRatio = malformedCount.toRatio(sampledCount)
        val canonicalKeyHitCount = parsedObjects.sumOf { jsonObject ->
            CanonicalFieldAliases.confidenceAliasGroups.count { group -> jsonObject.keys.any { key -> key in group } }
        }
        val maxCanonicalHits = max(1, successfulParseCount * CanonicalFieldAliases.confidenceAliasGroups.size)
        val canonicalKeyHitRatio = canonicalKeyHitCount.toRatio(maxCanonicalHits)
        val lowSamplePenalty = when {
            sampledCount == 1 && canonicalKeyHitCount == 0 -> lowSampleSingleRecordPenalty
            sampledCount <= 2 && canonicalKeyHitCount == 0 -> lowSampleDoubleRecordNoCanonicalPenalty
            sampledCount <= 2 -> lowSampleDoubleRecordCanonicalPenalty
            else -> 0.0
        }

        val finalScore = (
            (parseSuccessRatio * 0.65) +
                (canonicalKeyHitRatio * 0.35) -
                (malformedRatio * 0.45) -
                lowSamplePenalty
            ).coerceToUnitRange()

        return ParseDetectionConfidence(
            parserName = "JSON",
            sampledRecordCount = sampledCount,
            successfulParseCount = successfulParseCount,
            malformedCount = malformedCount,
            parseSuccessRatio = parseSuccessRatio,
            malformedRatio = malformedRatio,
            canonicalKeyHitCount = canonicalKeyHitCount,
            canonicalKeyHitRatio = canonicalKeyHitRatio,
            finalConfidenceScore = finalScore,
            debugFactors = mapOf(
                "parseSuccessRatio" to parseSuccessRatio,
                "canonicalKeyHitRatio" to canonicalKeyHitRatio,
                "malformedRatio" to malformedRatio,
                "lowSamplePenalty" to lowSamplePenalty
            )
        )
    }

    /**
     * Determines if the JSON format should be selected based on the provided parsing confidence.
     *
     * @param confidence the parsing confidence metrics that include various indicators of parsing success and quality.
     * @return `true` if the JSON format should be selected, otherwise `false`.
     */
    fun shouldSelectJson(confidence: ParseDetectionConfidence): Boolean {
        return confidence.successfulParseCount > 0 && confidence.finalConfidenceScore >= confidenceThreshold
    }

    /**
     * Converts an integer value to a ratio relative to the given total.
     * If the total is less than or equal to zero, the result will be 0.0.
     *
     * @param total The total value used to calculate the ratio. Must be greater than zero for valid ratios.
     * @return The ratio as a Double. If the total is less than or equal to zero, returns 0.0.
     */
    private fun Int.toRatio(total: Int): Double {
        if (total <= 0) return 0.0
        return toDouble() / total.toDouble()
    }

    /**
     * Ensures the value of the Double is constrained to the range [0.0, 1.0].
     * If the value is less than 0.0, it will be coerced to 0.0.
     * If the value is greater than 1.0, it will be coerced to 1.0.
     *
     * @return A Double value within the unit range [0.0, 1.0].
     */
    private fun Double.coerceToUnitRange(): Double = min(1.0, max(0.0, this))

    /**
     * Companion object for the `JsonConfidenceScorer` class.
     * Contains default configuration constants used for confidence scoring.
     */
    companion object {
        /**
         * The default confidence threshold value used to determine the minimum level of confidence
         * required for a JSON object to be considered valid during confidence scoring.
         *
         * This value is applied as a baseline threshold in scenarios where no specific confidence
         * threshold is provided. It influences the behavior of scoring and selection logic
         * within the JSON confidence scorer.
         */
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.45
        /**
         * The default penalty applied when only a single valid record is sampled with insufficient data.
         *
         * This constant is used by the scoring algorithm in scenarios where the sample size is too low
         * to draw confident conclusions about the structure or validity of the JSON logs. The penalty
         * helps to adjust the confidence score accordingly, ensuring that limited data results in
         * a reduced confidence.
         */
        private const val DEFAULT_LOW_SAMPLE_SINGLE_RECORD_PENALTY = 0.25
        /**
         * Represents the default penalty applied when scoring a double-record JSON input
         * that has low sampling and lacks a canonical representation.
         *
         * This constant is used to adjust the confidence score in cases where the absence
         * of a canonical structure in double-record data can lead to increased uncertainty.
         * The penalty is applied as a percentage, where a lower value decreases the influence
         * on the overall confidence score.
         */
        private const val DEFAULT_LOW_SAMPLE_DOUBLE_RECORD_NO_CANONICAL_PENALTY = 0.15
        /**
         * Default penalty value applied during confidence scoring when there is a low sample count,
         * and two records are detected with a canonical match.
         * This penalty serves to adjust the confidence score proportionally
         * to account for the reduced reliability of smaller data samples.
         */
        private const val DEFAULT_LOW_SAMPLE_DOUBLE_RECORD_CANONICAL_PENALTY = 0.05
    }
}
