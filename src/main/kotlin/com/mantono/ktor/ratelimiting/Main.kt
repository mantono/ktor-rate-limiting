package com.mantono.ktor.ratelimiting

import com.sun.xml.internal.ws.client.ContentNegotiation
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Principal
import io.ktor.auth.principal
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
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
				this.context.principal<Principal>() ?: "anonymous"
			}
		}
		routing {
			get("/notRateLimited") {
				call.respond(HttpStatusCode.OK)
			}
			get("/rateLimited1") {
				rateLimited {
					call.respond(HttpStatusCode.OK, "Rate limited")
				}
			}
		}
	}
}