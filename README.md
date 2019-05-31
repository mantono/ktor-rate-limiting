# ktor-rate-limiting
[Ktor](https://ktor.io) feature for [rate limiting](https://blog.apisyouwonthate.com/what-is-api-rate-limiting-all-about-1819a390ab06).
Limit the number of requests a user or client
can make within a certain time period. When all requests has been consumed, [status code 429](https://http.cat/429)
will be returned.

## Usage

### Minimum Working Example
Just install feature `RateLimiting`, no config is required.
```kotlin
fun main() {
	embeddedServer(Netty, port = 80) {
		install(RateLimiting)
		routing {
			get("/") {
				call.respond(HttpStatusCode.OK)
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
				call.respond(HttpStatusCode.OK)
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
By default the IP address of the request origin host is used, but it could be changed to example
be determined on a user name or token present in the request.