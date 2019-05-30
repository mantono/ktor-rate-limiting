package com.mantono.ktor.ratelimiting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class RateLimiterTest {


	@Test
	fun stampRateLimiterTest() {
		val limiter = RateLimiter<Int>(1000, Duration.ofHours(1L))
		val scope = CoroutineScope(Job())
		limiter.start(scope)
		runBlocking {
			limiter.consume(1)
			limiter.consume(1)
			limiter.consume(1)
		}
		Thread.sleep(5)
		assertEquals(997L, limiter.remaining(1))
	}
}