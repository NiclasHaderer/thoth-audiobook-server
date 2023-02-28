package io.thoth.common.scheduling

import java.time.Duration
import java.time.LocalDateTime

enum class TaskType {
    CRON, EVENT
}

typealias Task = suspend () -> Unit

class ScheduledTask(
    val task: TaskDescription, val executeAt: LocalDateTime, val type: TaskType, val cause: String
) {
    fun schedulesIn(): Long {
        val now = LocalDateTime.now()
        var ms = try {
            Duration.between(now, executeAt).toMillis()
        } catch (e: ArithmeticException) {
            Long.MAX_VALUE
        }
        if (now.isAfter(executeAt)) {
            ms *= -1
        }
        return ms
    }
}