package io.thoth.common.extensions

import com.cronutils.model.Cron
import com.cronutils.model.time.ExecutionTime
import java.time.LocalDateTime
import java.time.ZonedDateTime

fun Cron.nextExecution(): LocalDateTime {
    val nextExecutionTime = ExecutionTime.forCron(this).nextExecution(ZonedDateTime.now()).get()
    return nextExecutionTime.toLocalDateTime()
}
