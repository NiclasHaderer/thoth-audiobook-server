package io.thoth.server.common.scheduling

import com.cronutils.model.Cron
import io.thoth.server.common.extensions.toCron

enum class TaskType {
    CRON,
    EVENT
}

interface Task {
    val name: String
    val type: TaskType
}

class EventTask<T> internal constructor(override val name: String, internal val callback: suspend (Event<T>) -> Unit) :
    Task {
    override val type: TaskType = TaskType.EVENT

    class Event<T> internal constructor(val name: String, val data: T, internal val origin: EventTask<T>)

    fun build(data: T): Event<T> {
        return Event(name, data, this)
    }
}

class ScheduleTask
internal constructor(override val name: String, val cron: Cron, internal val callback: suspend () -> Unit) : Task {

    constructor(
        name: String,
        cronString: String,
        callback: suspend () -> Unit
    ) : this(
        name,
        cronString.toCron(),
        callback,
    )

    override val type: TaskType = TaskType.CRON
}

interface ScheduleCollection {
    fun <T> event(event: String, callback: suspend (EventTask.Event<T>) -> Unit): EventTask<T> {
        return EventTask(event, callback)
    }

    fun schedule(cronString: String, name: String, callback: suspend () -> Unit): ScheduleTask {
        return ScheduleTask(cronString, name, callback)
    }

    fun schedule(name: String, cron: Cron, callback: suspend () -> Unit): ScheduleTask {
        return ScheduleTask(name, cron, callback)
    }
}
