package com.mantono.ktor.ratelimiting

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertNull

class KtorFeatureTest {
	@Test
	fun `rate limit quota is consumed on request`(): Unit = withTestApplication {
		application.setup()

		val call: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
		}

		val remaining: Int? = call.response.headers["X-RateLimit-Remaining"]?.toInt()
		assertEquals(4, remaining)
	}

	@Test
	fun `multiple following request are counted towards rate limit quota`(): Unit = withTestApplication {
		application.setup()

		handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
		}

		val call: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
		}

		val remaining: Int? = call.response.headers["X-RateLimit-Remaining"]?.toInt()
		assertEquals(3, remaining)
	}

	@Test
	fun `request with options method is not counted towards quota`(): Unit = withTestApplication {
		application.setup()

		val callToOptions: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Options
			this.uri = "/"
		}

		assertNull(callToOptions.response.headers["X-RateLimit-Remaining"])

		val call: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
		}

		val remaining: Int? = call.response.headers["X-RateLimit-Remaining"]?.toInt()
		assertEquals(4, remaining)
	}

	@Test
	fun `request on health check endpoint is not counted towards quota`(): Unit = withTestApplication {
		application.setup()

		val callToHealthCheck: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/healthz"
		}

		assertNull(callToHealthCheck.response.headers["X-RateLimit-Remaining"])

		val call: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
		}

		val remaining: Int? = call.response.headers["X-RateLimit-Remaining"]?.toInt()
		assertEquals(4, remaining)
	}

	@Test
	fun `request with different key value should not count towards same quota`(): Unit = withTestApplication {
		application.setup()

		val call0: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
			this.addHeader("Authorization", "0")
		}

		val call1: TestApplicationCall = handleRequest {
			this.method = HttpMethod.Get
			this.uri = "/"
			this.addHeader("Authorization", "1")
		}

		assertEquals(4, call0.response.headers["X-RateLimit-Remaining"]?.toInt())
		assertEquals(4, call1.response.headers["X-RateLimit-Remaining"]?.toInt())
	}
}

internal fun Application.setup() {
	install(RateLimiting) {
		this.limit = 5
		this.resetTime = Duration.ofSeconds(20L)
		this.keyExtraction = {
			this.call.request.header("Authorization") ?: this.call.request.origin.remoteHost
		}
		this.pathExclusion = { method: HttpMethod, path: String ->
			method == HttpMethod.Options || path.endsWith("/healthz")
		}
	}
	routing {
		get("/") {
			call.respond(HttpStatusCode.OK, "Rate limited")
		}
	}
}