package io.thoth.server.common.scheduling

import io.github.oshai.kotlinlogging.KotlinLogging.logger
import io.thoth.server.common.extensions.toHumanReadable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.toKotlinDuration

private enum class SchedulerState {
    STOPPED,
    RUNNING,
    /** Stop was requested and the loop is finishing the task it is running */
    STOPPING,
}

class Scheduler {
    private val log = logger {}

    // Needs to be locked before reading/writing from it
    private val taskQueue = mutableListOf<ScheduledTask>()
    private val state = AtomicReference(SchedulerState.STOPPED)

    private val queueChanged = Channel<Unit>(Channel.CONFLATED)

    val queue: List<QueuedTask>
        get() =
            synchronized(taskQueue) {
                taskQueue.map { QueuedTask(it.task.name, it.executeAt, it.task.type) }
            }

    suspend fun start() {
        if (!state.compareAndSet(SchedulerState.STOPPED, SchedulerState.RUNNING)) {
            log.warn { "Scheduler is already running" }
            return
        }
        try {
            runQueue()
        } finally {
            state.set(SchedulerState.STOPPED)
        }
    }

    fun stop() {
        state.compareAndSet(SchedulerState.RUNNING, SchedulerState.STOPPING)
        wakeUp()
    }

    fun schedule(task: CronTask) {
        // Constructed before locking, as it evaluates the cron expression
        val execution = ScheduledCronTask(task)
        synchronized(taskQueue) {
            // One cron entry per task, so scheduling the same task twice does not double its executions
            taskQueue.removeAll { it is ScheduledCronTask && it.task === task }
            taskQueue.add(execution)
        }
        wakeUp()
    }

    fun launchNow(task: CronTask) {
        synchronized(taskQueue) { taskQueue.add(ScheduledManualTask(task)) }
        wakeUp()
        log.info { "Queued schedule '${task.name}'" }
    }

    /** Runs the task the event was built by. */
    fun <T> dispatch(event: EventTask.Event<T>) {
        check(state.get() == SchedulerState.RUNNING) { "Scheduler not started" }

        synchronized(taskQueue) { taskQueue.add(ScheduledEventTask(event)) }
        wakeUp()
        log.info { "Dispatched event '${event.name}' to schedule '${event.origin.name}'" }
    }

    private fun wakeUp() {
        queueChanged.trySend(Unit)
    }

    private fun rescheduleCronTask(task: CronTask) {
        // Runs from a finally block, so evaluating the cron must not be able to take the scheduler down
        val execution =
            try {
                ScheduledCronTask(task)
            } catch (e: Exception) {
                log.error(e) { "Could not reschedule task '${task.name}'. It will not run again." }
                return
            }
        synchronized(taskQueue) {
            // Rescheduling twice would pile up cron entries, which someone scheduling the task again can cause
            if (taskQueue.any { it is ScheduledCronTask && it.task === task }) return
            taskQueue.add(execution)
        }
    }

    private suspend fun runQueue() {
        while (state.get() == SchedulerState.RUNNING && currentCoroutineContext().isActive) {
            // The task which is due next. Its execution time can be in the past if it is overdue.
            val next = synchronized(taskQueue) { taskQueue.minByOrNull { it.executeAt } }

            if (next == null) {
                log.debug { "No tasks in queue. Waiting for new tasks to be scheduled" }
                queueChanged.receive()
                continue
            }

            val waitFor = next.timeUntilExecution()
            if (waitFor > Duration.ZERO) {
                log.debug {
                    "Next task '${next.task.name}' will be executed in ${waitFor.toHumanReadable()}. " +
                        "Triggered by ${next.task.type}:${next.cause}"
                }
                // Waking up early means the queue changed and another task may be due before this one
                if (withTimeoutOrNull(waitFor.toKotlinDuration()) { queueChanged.receive() } != null) continue
            }

            // Take the task out of the queue before running it, so nothing can queue it a second time meanwhile
            if (!synchronized(taskQueue) { taskQueue.remove(next) }) continue
            execute(next)
        }
    }

    private suspend fun execute(scheduledTask: ScheduledTask) {
        try {
            // The tasks block, as they read files and talk to the database
            withContext(Dispatchers.IO) { scheduledTask.run() }
            log.debug { "Scheduled task '${scheduledTask.task.name}' was executed successfully" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A failing task must neither take the scheduler down with it nor be retried in a tight loop
            log.error(e) { "Scheduled task '${scheduledTask.task.name}' failed" }
        } finally {
            if (scheduledTask is ScheduledCronTask) {
                rescheduleCronTask(scheduledTask.task)
            }
        }
    }
}
