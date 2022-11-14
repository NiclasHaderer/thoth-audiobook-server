package io.thoth.server.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import mu.KotlinLogging.logger

class Scheduler(parallelism: Int) {
    private val semaphore = Semaphore(parallelism)
    private val log = logger {}

    suspend fun <T> queueAsync(callback: suspend () -> T): Deferred<T?> {
        semaphore.acquire()
        return CoroutineScope(Dispatchers.IO).async {
            val deferred = async {
                try {
                    callback()
                } catch (e: Exception) {
                    this@Scheduler.log.error(e) { "Queued task caused an exception" }
                    null
                }
            }
            return@async try {
                deferred.await()
            } finally {
                semaphore.release()
            }
        }
    }

    @Suppress("DeferredResultUnused")
    suspend fun queue(callback: suspend () -> Unit) {
        this.queueAsync(callback)
    }
}
