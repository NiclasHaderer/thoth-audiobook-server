package io.thoth.server.common.scheduling

import io.thoth.server.common.extensions.nextExecution
import java.time.Duration
import java.time.LocalDateTime

data class QueuedTask(
    val name: String,
    val executeAt: LocalDateTime,
    val type: TaskType,
)

abstract class ScheduledTask(
    open val task: Task,
    val executeAt: LocalDateTime,
    val cause: String,
) {
    fun timeUntilExecution(): Duration = Duration.between(LocalDateTime.now(), executeAt)

    abstract suspend fun run()
}

class ScheduledCronTask(
    override val task: CronTask,
) : ScheduledTask(task, task.cron.nextExecution(), task.cron.asString()) {
    override suspend fun run() {
        task.callback()
    }
}

class ScheduledManualTask(
    override val task: CronTask,
) : ScheduledTask(task, LocalDateTime.now(), "Launched manually") {
    override suspend fun run() {
        task.callback()
    }
}

class ScheduledEventTask<T>(
    val event: EventTask.Event<T>,
) : ScheduledTask(event.origin, LocalDateTime.now(), event.name) {
    override suspend fun run() {
        event.origin.callback(event)
    }
}
