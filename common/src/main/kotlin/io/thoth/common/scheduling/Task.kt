package io.thoth.common.scheduling

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser

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
internal constructor(override val name: String, val cronString: String, internal val callback: suspend () -> Unit) :
    Task {
    override val type: TaskType = TaskType.CRON
    val cron: Cron by lazy {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
        val parser = CronParser(cronDefinition)
        parser.parse(cronString)
    }
}

interface ScheduleCollection {
    fun <T> event(event: String, callback: suspend (EventTask.Event<T>) -> Unit): EventTask<T> {
        return EventTask(event, callback)
    }

    fun schedule(cronString: String, name: String, callback: suspend () -> Unit): ScheduleTask {
        return ScheduleTask(cronString, name, callback)
    }
}
