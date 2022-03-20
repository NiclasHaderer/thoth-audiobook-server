package io.github.huiibuh.utils

import io.github.huiibuh.extensions.classLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore

class Scheduler(parallelism: Int) {
    private val semaphore = Semaphore(parallelism)
    private val log = classLogger()

    suspend fun <T> queueAsync(callback: suspend () -> T): Deferred<T?> {
        semaphore.acquire()
        return CoroutineScope(Dispatchers.IO).async {
            val deferred = async {
                try {
                    callback()
                } catch (e: Exception) {
                    this@Scheduler.log.error("Queued task caused an exception", e)
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
    suspend fun queue(callback: suspend () -> Any) {
        this.queueAsync(callback)
    }
}
