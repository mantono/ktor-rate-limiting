package com.mantono.ktor.ratelimiting

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.time.Duration
import java.time.Instant

class RateLimiting private constructor(
	internal val rateLimit: RateLimiter<Any>,
	internal val keyExtraction: PipelineContext<Unit, ApplicationCall>.() -> Any
) {

	class Configuration {
		var limit: Long = 1000L
		var resetTime: Duration = Duration.ofHours(1L)
		var keyExtraction: PipelineContext<Unit, ApplicationCall>.() -> Any = {
			this.call.request.origin.remoteHost
		}
	}

	companion object Feature: ApplicationFeature<ApplicationCallPipeline, Configuration, RateLimiting> {

		/**
		 * Request limit per hour
		 */
		private const val HEADER_LIMIT = "X-RateLimit-Limit"

		/**
		 * The number of requests left in the current time window
		 */
		private const val HEADER_REMAINING = "X-RateLimit-Remaining"

		/**
		 * UNIX epoch second timestamp at which the request quota is reset
		 */
		private const val HEADER_RESET = "X-RateLimit-Reset"

		/**
		 * Indicate how long to wait before the client should make a
		 * new attempt to connect. Unlike [HEADER_RESET], this is expressed
		 * as seconds in _relative_ time from the current time.
		 */
		private const val HEADER_RETRY = "Retry-After"

		override val key: AttributeKey<RateLimiting> = AttributeKey("RateLimiting")

		override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): RateLimiting {
			val config = Configuration().apply(configure)
			val limiter: RateLimiter<Any> = RateLimiter(config.limit, config.resetTime)
			val rateLimiting = RateLimiting(limiter, config.keyExtraction)

			pipeline.intercept(ApplicationCallPipeline.Call) {
				val key: Any = rateLimiting.keyExtraction(this)
				val allow: Boolean = rateLimiting.rateLimit.allow(key)
				rateLimiting.rateLimit.consume(key)
				val rate: Rate = rateLimiting.rateLimit[key]

				context.response.header(HEADER_LIMIT, config.limit)
				context.response.header(HEADER_REMAINING, rate.remainingRequests)
				context.response.header(HEADER_RESET, rate.resetsAt.epochSecond)
				if(!allow) {
					val retryAt: Long = (rate.resetsAt.epochSecond - Instant.now().epochSecond).coerceAtLeast(0)
					context.response.header(HEADER_RETRY, retryAt)
					call.respond(HttpStatusCode.TooManyRequests)
					finish()
				}
			}

			return rateLimiting
		}
	}
}