package io.thoth.common.scheduling

import io.thoth.common.extensions.nextExecution
import io.thoth.common.extensions.toHumanReadable
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging.logger

class EventBuilder<T>
internal constructor(val name: String, internal val callback: suspend (EventBuilder.Event<T>) -> Unit) {

    class Event<T> internal constructor(val name: String, val data: T, internal val builder: EventBuilder<T>)

    fun build(data: T): Event<T> {
        return Event(name, data, this)
    }
}

class ScheduleBuilder(val name: String, val cronString: String, val callback: suspend () -> Unit)

interface SchedulingCollection {
    fun <T> event(event: String, callback: suspend (EventBuilder.Event<T>) -> Unit): EventBuilder<T> {
        return EventBuilder(event, callback)
    }

    fun schedule(cronString: String, name: String, callback: suspend () -> Unit): ScheduleBuilder {
        return ScheduleBuilder(cronString, name, callback)
    }
}

open class Scheduler {
    private var currentExecution: DelayedExecution? = null
    private val schedules = mutableListOf<TaskDescription>()
    private val log = logger {}
    private val started = AtomicBoolean(false)

    private val taskQueue = mutableListOf<ScheduledTask>()

    suspend fun start() {
        if (started.get()) {
            return
        }
        started.set(true)
        internalStart()
    }

    fun launchScheduledJob(schedule: ScheduleBuilder) {
        launchScheduledJob(schedule.name)
    }

    fun launchScheduledJob(scheduleName: String) {
        val relevantSchedules = schedules.filterIsInstance<CronTaskDescription>().filter { it.name == scheduleName }
        relevantSchedules.forEach { task ->
            val taskInSchedule = taskQueue.find { it.task == task }
            // The task is already overdue, so we don't schedule it again
            if (taskInSchedule != null && taskInSchedule.schedulesIn() < 0) {
                log.info { "Task ${task.name} is already scheduled for next possible execution" }
            } else {
                taskQueue.add(ScheduledCronTask(task, LocalDateTime.now(), "Launched manually"))
            }
        }
        if (relevantSchedules.isEmpty()) {
            log.warn { "No schedules for scheduleName $scheduleName" }
        } else {
            log.info { "Dispatched schedule $scheduleName to ${relevantSchedules.size} schedules" }
        }
    }

    suspend fun <T> dispatch(event: EventBuilder.Event<T>) {
        if (!started.get()) {
            throw IllegalStateException("Scheduler not started")
        }

        val relevantSchedules =
            schedules.filterIsInstance<EventTaskDescription<T>>().filter { it.event == event.builder }

        relevantSchedules.forEach { task -> taskQueue.add(ScheduledEventTask(task, event)) }
        if (relevantSchedules.isEmpty()) {
            log.warn { "No schedules for event $event" }
        } else {
            log.info { "Dispatched event $event to ${relevantSchedules.size} schedules" }
        }
        reevaluateNextExecutiontime()
    }

    fun <T> register(event: EventBuilder<T>) {
        val newTask = EventTaskDescription(name = event.name, runner = event.callback, event = event)
        schedules.add(newTask)
    }

    suspend fun schedule(schedule: ScheduleBuilder) {
        schedule(schedule.cronString, schedule.name, schedule.callback)
    }

    suspend fun schedule(cronString: String, name: String? = null, task: suspend () -> Unit) {
        val newTask =
            CronTaskDescription(
                name = name ?: task.toString(),
                cronString = cronString,
                runner = task,
            )
        schedules.add(newTask)
        queueTask(newTask)
        reevaluateNextExecutiontime()
    }

    private fun queueTask(task: CronTaskDescription) {
        val nextExecution = task.cron.nextExecution()
        taskQueue.add(ScheduledCronTask(task, nextExecution))
    }

    private suspend fun reevaluateNextExecutiontime() {
        currentExecution?.cancel()
        currentExecution = null
    }

    private suspend fun internalStart() = coroutineScope {
        while (true) {
            // Get the task with the next execution time. This time can also be in the past if the
            // task is
            // overdue.
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
                    "Triggered by ${scheduledTask.type}:${scheduledTask.cause}"
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
                taskQueue.filter { it == scheduledTask }.forEach { taskQueue.remove(it) }
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
