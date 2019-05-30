package com.mantono.ktor.ratelimiting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Semaphore

class RateLimiter<in T: Any>(
	val limit: Long = 1000,
	val resetTime: Duration = Duration.ofHours(1),
	initialSize: Int = 64
) {
	private val records: MutableMap<T, Rate> = HashMap(initialSize)
	private val rateConsumption: Channel<T> = Channel(100)
	private val start: Semaphore = Semaphore(1)
	private var lastPurge: Instant = Instant.now()

	suspend fun consume(key: T): Instant {
		rateConsumption.send(key)
		return resetsAt(key)
	}

	suspend fun consume(
		key: T,
		onReject: suspend () -> Unit,
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
			onReject()
			false
		}
	}

	fun allow(key: T): Boolean = remaining(key) > 0 || isTimeToReset(key)

	fun resetsAt(key: T): Instant {
		return records[key]?.resetsAt ?: Instant.now().plus(resetTime)
	}

	private fun isTimeToReset(key: T): Boolean = resetsAt(key) <= Instant.now()

	private fun resetRateLimit(key: T) {
		records[key] = defaultRate()
	}

	fun remaining(key: T): Long = records[key]?.remainingRequests ?: limit

	operator fun get(key: T): Rate = records.getOrDefault(key, defaultRate())

	fun start(coroutineScope: CoroutineScope) {
		if(start.tryAcquire()) {
			coroutineScope.launch { process() }
		}
	}

	private tailrec suspend fun process() {
		val consumer: T = rateConsumption.receive()
		val newRate: Rate = records.getOrDefault(consumer, defaultRate()).consume()
		records[consumer] = newRate
		if(timeToPurge()) {
			purge()
		}
		process()
	}

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
}