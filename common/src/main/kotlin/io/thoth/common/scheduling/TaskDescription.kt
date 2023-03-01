package io.thoth.common.scheduling

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser

abstract class TaskDescription(
    val name: String
)

class CronTaskDescription(
    name: String,
    val runner: suspend () -> Unit,
    cronString: String,
) : TaskDescription(name) {
    val cron: Cron by lazy {
        val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
        val parser = CronParser(cronDefinition)
        parser.parse(cronString)
    }
}

class EventTaskDescription<T>(
    name: String,
    val runner: suspend (EventBuilder.Event<T>) -> Unit,
    val event: EventBuilder<T>
) : TaskDescription(name)