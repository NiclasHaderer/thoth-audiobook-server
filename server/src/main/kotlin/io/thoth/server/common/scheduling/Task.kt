package io.thoth.server.common.scheduling

import com.cronutils.model.Cron

enum class TaskType {
    CRON,
    EVENT,
}

interface Task {
    val name: String
    val type: TaskType
}

class EventTask<T>(
    override val name: String,
    val callback: suspend (Event<T>) -> Unit,
) : Task {
    override val type = TaskType.EVENT

    class Event<T>(
        val name: String,
        val data: T,
        val origin: EventTask<T>,
    )

    fun build(data: T): Event<T> = Event(name, data, this)
}

class CronTask(
    override val name: String,
    val cron: Cron,
    val callback: suspend () -> Unit,
) : Task {
    override val type = TaskType.CRON
}
