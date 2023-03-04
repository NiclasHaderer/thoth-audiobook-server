package io.thoth.common.scheduling

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

class DelayedExecution(private val delay: Long, private val task: suspend () -> Unit) {
    private val isRunning = AtomicBoolean(false)
    private var ranSuccessful = false

    private lateinit var job: Job

    suspend fun runInBackground(scope: CoroutineScope) {
        job =
            scope.launch {
                try {
                    if (this@DelayedExecution.delay > 0) {
                        delay(this@DelayedExecution.delay)
                    }
                    this@DelayedExecution.isRunning.set(true)
                    this@DelayedExecution.task()
                    this@DelayedExecution.ranSuccessful = true
                } catch (e: CancellationException) {
                    // Do nothing
                }
            }
    }

    fun isRunning(): Boolean {
        return isRunning.get()
    }

    fun isWaiting(): Boolean {
        return !isRunning.get()
    }

    fun executedSuccessfully(): Boolean {
        return ranSuccessful
    }

    suspend fun join() {
        job.join()
    }

    suspend fun cancel() {
        if (!isRunning.get()) {
            job.cancel()
        } else {
            join()
        }
    }
}
