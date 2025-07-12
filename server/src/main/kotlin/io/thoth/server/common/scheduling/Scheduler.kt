package io.thoth.server.common.scheduling

import io.thoth.server.common.extensions.nextExecution
import io.thoth.server.common.extensions.toHumanReadable
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging.logger

abstract class TaskQueueHolder {
    private val _taskQueue = mutableListOf<ScheduledTask>()

    protected val taskQueue: List<ScheduledTask>
        get() = modifyQueue { toList() }

    protected fun <T> modifyQueue(action: MutableList<ScheduledTask>.() -> T): T {
        synchronized(_taskQueue) {
            return _taskQueue.action()
        }
    }
}

open class Scheduler : TaskQueueHolder() {
    data class QueuedTask(val name: String, val executeAt: LocalDateTime, val type: TaskType)

    private var currentExecution: DelayedExecution? = null
    private val schedules = mutableListOf<Task>()
    private val log = logger {}
    private val started = AtomicBoolean(false)

    val queue: List<QueuedTask>
        get() = taskQueue.map { QueuedTask(it.task.name, it.executeAt, it.task.type) }

    suspend fun start() {
        if (!started.compareAndExchange(false, true)) {
            return
        }
        internalStart()
    }

    fun launchScheduledJob(schedule: ScheduleTask) {
        val relevantSchedules = schedules.filterIsInstance<ScheduleTask>().filter { it == schedule }
        relevantSchedules.forEach { task ->
            modifyQueue { add(ScheduledCronTask(task, LocalDateTime.now(), "Launched manually")) }
        }
        if (relevantSchedules.isEmpty()) {
            log.warn { "No schedules for scheduleName ${schedule.name}" }
        } else {
            log.info { "Dispatched schedule ${schedule.name} to ${relevantSchedules.size} schedules" }
        }
    }

    suspend fun <T> dispatch(event: EventTask.Event<T>) {
        if (!started.get()) {
            throw IllegalStateException("Scheduler not started")
        }

        val relevantSchedules = schedules.filterIsInstance<EventTask<T>>().filter { it == event.origin }

        relevantSchedules.forEach { task -> modifyQueue { add(ScheduledEventTask(task, event)) } }
        if (relevantSchedules.isEmpty()) {
            log.warn { "No schedules for event $event" }
        } else {
            log.info { "Dispatched event $event to ${relevantSchedules.size} schedules" }
        }
        reevaluateNextExecutionTime()
    }

    fun <T> register(event: EventTask<T>) {
        schedules.add(event)
    }

    suspend fun schedule(schedule: ScheduleTask) {
        schedules.add(schedule)
        queueTask(schedule)
        reevaluateNextExecutionTime()
    }

    private fun queueTask(task: ScheduleTask) {
        val nextExecution = task.cron.nextExecution()
        modifyQueue { add(ScheduledCronTask(task, nextExecution)) }
    }

    private suspend fun reevaluateNextExecutionTime() {
        currentExecution?.cancel()
        currentExecution = null
    }

    private suspend fun internalStart() = coroutineScope {
        while (true) {
            // Get the task with the next execution time. This time can also be in the past if the
            // task is overdue.
            val scheduledTask = taskQueue.minByOrNull { it.executeAt }

            if (scheduledTask == null) {
                log.debug { "No tasks in queue. Waiting for new tasks to be scheduled" }
                val currentExecution = DelayedExecution(Long.MAX_VALUE) {}
                currentExecution.runInBackground(this@coroutineScope)
                this@Scheduler.currentExecution = currentExecution
                currentExecution.join()
                continue
            }

            log.debug {
                "Next task '${scheduledTask.task.name}' will be executed in ${
                    Duration.of(scheduledTask.schedulesIn(), ChronoUnit.MILLIS).toHumanReadable()
                }. " +
                    "Triggered by ${scheduledTask.task.type}:${scheduledTask.cause}"
            }

            // Schedule the task for execution
            val currentExecution = DelayedExecution(scheduledTask.schedulesIn(), scheduledTask::run)
            currentExecution.runInBackground(this@coroutineScope)
            this@Scheduler.currentExecution = currentExecution
            // Wait for the task to be executed
            currentExecution.join()

            if (currentExecution.executedSuccessfully()) {
                // Task has been executed successfully
                // Task can therefore be removed from the queue
                log.debug { "Scheduled task '${scheduledTask.task.name}' was executed successfully" }
                modifyQueue { filter { it == scheduledTask }.forEach { remove(it) } }
                // If the task was a cron task, it should be rescheduled
                if (scheduledTask is ScheduledCronTask) {
                    queueTask(scheduledTask.task)
                }
            } else {
                // Task was canceled
                // This can happen if a new task is added to the schedule or an event is dispatched
                // In this case just look for the next task to run by checking the queue again
                log.debug { "Waiting for scheduled task was canceled. Looking for the next task to run" }
            }
        }
    }
}
