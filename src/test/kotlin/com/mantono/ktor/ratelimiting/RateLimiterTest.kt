package com.mantono.ktor.ratelimiting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimiterTest {
    @Test
    fun stampRateLimiterTest() {
        val job = Job()
        val scope = CoroutineScope(job)
        val limiter = RateLimiter<Int>(10_000, Duration.ofHours(1L))

        val results: MutableList<Job> = ArrayList(6000)
        repeat(5000) {
            results.add(scope.launch { limiter.consume(1) })
        }
        runBlocking {
            results.forEach { it.join() }
            assertEquals(5000L, limiter.remaining(1))
        }
        scope.cancel()
    }
}