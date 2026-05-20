package com.klogviewer.core.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val logger = KotlinLogging.logger {}

suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 500,
    maxDelay: Long = 2000,
    factor: Double = 2.0,
    shouldRetry: (Exception) -> Boolean = { true },
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries - 1) { attemptNum ->
        try {
            return block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (!shouldRetry(e)) throw e
            
            logger.warn { "Attempt ${attemptNum + 1} failed: ${e.message}. Retrying in ${currentDelay}ms..." }
            delay(currentDelay.milliseconds)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // Final attempt
}
