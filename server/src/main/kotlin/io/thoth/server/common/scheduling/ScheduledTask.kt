package io.thoth.server.common.scheduling

import java.time.Duration
import java.time.LocalDateTime

internal abstract class ScheduledTask(
    open val task: Task,
    val executeAt: LocalDateTime,
    val cause: String,
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
    override val task: ScheduleTask,
    executeAt: LocalDateTime,
    cause: String = task.cron.asString()
) : ScheduledTask(task, executeAt, cause) {
    override suspend fun run() {
        task.callback()
    }
}

internal class ScheduledEventTask<T>(override val task: EventTask<T>, val event: EventTask.Event<T>) :
    ScheduledTask(task, LocalDateTime.now(), event.name) {
    override suspend fun run() {
        task.callback(event)
    }
}
