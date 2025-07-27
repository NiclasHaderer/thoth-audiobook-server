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

class EventTask<T>(override val name: String, val callback: suspend (Event<T>) -> Unit) : Task {
    override val type: TaskType = TaskType.EVENT

    class Event<T>(val name: String, val data: T, val origin: EventTask<T>)

    fun build(data: T): Event<T> {
        return Event(name, data, this)
    }
}

class ScheduleTask(override val name: String, val cron: Cron, val callback: suspend () -> Unit) : Task {
    override val type: TaskType = TaskType.CRON
}

interface ScheduleCollection {
    fun <T> event(event: String, callback: suspend (EventTask.Event<T>) -> Unit): EventTask<T> {
        return EventTask(event, callback)
    }

    fun schedule(name: String, cron: Cron, callback: suspend () -> Unit): ScheduleTask {
        return ScheduleTask(name, cron, callback)
    }
}
