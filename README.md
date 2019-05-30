# ktor-rate-limiting
[Ktor](https://ktor.io) feature for rate limiting

## Usage

### Minimum Working Example
Just install feature `RateLimiting` and guard any code that should be
rate limited inside a `rateLimited { ... }` block
```kotlin
fun main() {
	embeddedServer(Netty, port = 80) {
		install(RateLimiting)
		routing {
			get("/") {
				rateLimited {
					call.respond(HttpStatusCode.OK)
				}
			}
		}
	}.start(wait = true)
}
```

### Advanced Example
The example below shows what parameters can be configured for the feature.
Displayed values are the default values.
```kotlin
fun main() {
	embeddedServer(Netty, port = 80) {
		install(RateLimiting) {
			this.limit = 1000
			this.resetTime = Duration.ofHours(1L)
			this.keyExtraction = {
				this.call.request.origin.remoteHost
			}
		}
		routing {
			get("/") {
				rateLimited {
					call.respond(HttpStatusCode.OK)
				}
			}
		}
	}.start(wait = true)
}
```
- `limit` - the amount of requests that can be done before the request is denied
- `resetTime` - how long time that will elapse from the first request until the
request counter is rest
- `keyExtraction` - a lambda function of type `PipelineContext<Unit, ApplicationCall>.() -> Any` which
is used to determine what key in the request is used for determining ownership of request quota.
By default the IP address of the request origin host is used