package io.thoth.common.scheduling

import java.time.Duration
import java.time.LocalDateTime

enum class TaskType {
    CRON,
    EVENT
}

internal abstract class ScheduledTask(
    open val task: TaskDescription,
    val executeAt: LocalDateTime,
    val cause: String,
    val type: TaskType = TaskType.CRON
) {

    fun schedulesIn(): Long {
        val now = LocalDateTime.now()
        return try {
            Duration.between(now, executeAt).toMillis()
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }
    }

    abstract suspend fun run()
}

internal class ScheduledCronTask(
    override val task: CronTaskDescription,
    executeAt: LocalDateTime,
    cause: String = task.cron.asString()
) : ScheduledTask(task, executeAt, cause, TaskType.CRON) {
    override suspend fun run() {
        task.runner()
    }
}

internal class ScheduledEventTask<T>(override val task: EventTaskDescription<T>, val event: EventBuilder.Event<T>) :
    ScheduledTask(task, LocalDateTime.now(), event.name, TaskType.EVENT) {
    override suspend fun run() {
        task.runner(event)
    }
}
