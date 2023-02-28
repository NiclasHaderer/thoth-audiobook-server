package io.thoth.common.scheduling

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser

class TaskDescription(
    val name: String,
    val cron: Cron?,
    val task: Task,
) {
    constructor(
        name: String,
        cronString: String?,
        task: Task,
    ) : this(
        name, cronString?.let { CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(it) }, task
    )
}
