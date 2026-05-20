package com.klogviewer.core.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class RetryUtilsTest {

    @Test
    fun `should return value on first success`() = runTest {
        val attempts = AtomicInteger(0)
        val result = withRetry(maxRetries = 3) {
            attempts.incrementAndGet()
            "success"
        }
        
        expectThat(result).isEqualTo("success")
        expectThat(attempts.get()).isEqualTo(1)
    }

    @Test
    fun `should retry on failure and eventually succeed`() = runTest {
        val attempts = AtomicInteger(0)
        val result = withRetry(maxRetries = 3, initialDelay = 1) {
            val count = attempts.incrementAndGet()
            if (count < 2) throw IOException("Fail")
            "success"
        }
        
        expectThat(result).isEqualTo("success")
        expectThat(attempts.get()).isEqualTo(2)
    }

    @Test
    fun `should throw last exception after all retries fail`() = runTest {
        val attempts = AtomicInteger(0)
        try {
            withRetry(maxRetries = 3, initialDelay = 1) {
                attempts.incrementAndGet()
                throw IOException("Permanent failure")
            }
        } catch (e: IOException) {
            expectThat(e.message).isEqualTo("Permanent failure")
        }
        
        expectThat(attempts.get()).isEqualTo(3)
    }
}
