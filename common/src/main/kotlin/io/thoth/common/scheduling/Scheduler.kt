package io.thoth.common.scheduling

import io.thoth.common.extensions.nextExecution
import io.thoth.common.extensions.toHumanReadable
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging.logger
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean


class Scheduler {
    private var currentExecution: DelayedExecution? = null
    private val schedules = mutableListOf<TaskDescription>()
    private val log = logger {}
    private val started = AtomicBoolean(false)

    private val taskQueue = mutableListOf<ScheduledTask>()

    private val waitForever by lazy {
        ScheduledTask(
            task = TaskDescription(name = "no-tasks-in-queue", cronString = null, task = {}),
            executeAt = LocalDateTime.MAX,
            type = TaskType.EVENT,
            cause = "no-tasks-in-queue"
        )
    }

    suspend fun start() {
        if (started.get()) {
            return
        }
        started.set(true)
        internalStart()
    }

    suspend fun dispatchEvent(event: String) {
        if (!started.get()) {
            throw IllegalStateException("Scheduler not started")
        }

        val relevantSchedules = schedules.filter { it.name == event }
        relevantSchedules.forEach { task ->
            taskQueue.add(ScheduledTask(task, LocalDateTime.now(), TaskType.EVENT, event))
        }
        if (relevantSchedules.isEmpty()) {
            log.warn { "No schedules for event $event" }
        } else {
            log.info { "Dispatched event $event to ${relevantSchedules.size} schedules" }
        }
        reevaluateNextExecutiontime()
    }

    suspend fun schedule(name: String, cronString: String? = null, task: Task) {
        val newTask = TaskDescription(
            name = name,
            cronString = cronString,
            task = task,
        )
        schedules.add(newTask)
        queueTask(newTask)
        reevaluateNextExecutiontime()
    }

    private fun queueTask(task: TaskDescription) {
        if (task.cron != null) {
            val nextExecution = task.cron.nextExecution()
            taskQueue.add(ScheduledTask(task, nextExecution, TaskType.CRON, task.cron.asString()))
        }
    }

    private suspend fun reevaluateNextExecutiontime() {
        currentExecution?.cancel()
        currentExecution = null
    }

    private suspend fun internalStart() = coroutineScope {
        while (true) {
            // Get the task with the next execution time. This time can also be in the past if the task is overdue.
            val scheduledTask = taskQueue.minByOrNull { it.schedulesIn() } ?: waitForever
            val task = scheduledTask.task

            log.debug {
                "Next task '${task.name}' will be executed in ${
                    Duration.of(scheduledTask.schedulesIn(), ChronoUnit.MILLIS).toHumanReadable()
                }. " + "Triggered by ${scheduledTask.type}:${scheduledTask.cause}"
            }

            // Schedule the task for execution
            val currentExecution = DelayedExecution(scheduledTask.schedulesIn(), task.task)
            currentExecution.runInBackground(this@coroutineScope)
            this@Scheduler.currentExecution = currentExecution
            // Wait for the task to be executed
            currentExecution.join()

            if (currentExecution.executedSuccessfully()) {
                // Task has been executed successfully
                // Task can therefore be removed from the queue
                log.debug { "Scheduled task '${task.name}' was executed successfully" }
                taskQueue.filter { it == scheduledTask }.forEach { taskQueue.remove(it) }
                // If the task was a cron task, it should be rescheduled
                if (scheduledTask.type == TaskType.CRON) {
                    queueTask(task)
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