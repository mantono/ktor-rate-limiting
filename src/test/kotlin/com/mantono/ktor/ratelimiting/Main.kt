package com.mantono.ktor.ratelimiting

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Duration

fun main() {
	httpServer(9999).start(true)
}

fun httpServer(port: Int = 80): ApplicationEngine {
	return embeddedServer(Netty, port = port) {
		install(RateLimiting) {
			this.limit = 5
			this.resetTime = Duration.ofSeconds(20L)
			this.keyExtraction = {
				this.call.request.origin.remoteHost
			}
		}
		routing {
			get("/") {
				call.respond(HttpStatusCode.OK, "Rate limited")
			}
		}
	}
}