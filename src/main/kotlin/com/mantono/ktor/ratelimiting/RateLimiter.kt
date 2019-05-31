package com.mantono.ktor.ratelimiting

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class RateLimiter<in T: Any>(
	val limit: Long = 1000,
	val resetTime: Duration = Duration.ofHours(1),
	initialSize: Int = 64
) {
	private val records: ConcurrentMap<T, Rate> = ConcurrentHashMap(initialSize)
	private var lastPurge: Instant = Instant.now()

	fun consume(key: T): Rate {
		records.compute(key) { _, rate: Rate? ->
			val presentRate: Rate = rate?.resetIfNeeded(limit, resetTime) ?: defaultRate()
			presentRate.consume()
		}

		if(timeToPurge()) {
			purge()
		}

		return records[key]!!
	}

	suspend fun consume(
		key: T,
		onAllow: suspend () -> Unit
	): Boolean {
		if(isTimeToReset(key)) {
			resetRateLimit(key)
		}
		return if(allow(key)) {
			consume(key)
			onAllow()
			true
		} else {
			false
		}
	}

	fun allow(key: T): Boolean = remaining(key) > 0 || isTimeToReset(key)

	suspend fun ifAllowed(
		key: T,
		onAllow: suspend () -> Unit
	): Boolean {
		return if(allow(key)) {
			onAllow()
			true
		} else {
			false
		}
	}

	fun resetsAt(key: T): Instant {
		return records[key]?.resetsAt ?: Instant.now().plus(resetTime)
	}

	private fun isTimeToReset(key: T): Boolean = resetsAt(key) <= Instant.now()

	private fun resetRateLimit(key: T) {
		records[key] = defaultRate()
	}

	fun remaining(key: T): Long = records[key]?.remainingRequests ?: limit

	operator fun get(key: T): Rate = records.getOrDefault(key, defaultRate())

	private fun timeToPurge(): Boolean =
		records.size > 100 && timeSinceLastPurge() > Duration.ofMinutes(20L)

	private fun timeSinceLastPurge(): Duration = Duration.between(lastPurge, Instant.now())

	private fun purge() {
		val oldRecords: Collection<T> = records.filter { it.value.isReset() }.keys
		oldRecords.forEach {
			records.remove(it)
		}
	}

	private fun defaultRate(): Rate = Rate(Instant.now().plus(resetTime), limit)
}

data class Rate(
	val resetsAt: Instant,
	val remainingRequests: Long
) {
	fun isDepleted(): Boolean = remainingRequests <= 0
	fun consume(): Rate = if(isDepleted()) this else this.copy(remainingRequests = remainingRequests - 1)
	fun isReset(time: Instant = Instant.now()): Boolean = resetsAt <= time

	fun resetIfNeeded(limit: Long, resetTime: Duration): Rate =
		if(isReset()) Rate(Instant.now().plus(resetTime), limit) else this
}