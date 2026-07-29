package io.thoth.server.common.extensions

import com.cronutils.model.Cron
import com.cronutils.model.time.ExecutionTime
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** @throws IllegalStateException if the expression parses but can never fire again, e.g. "0 0 30 2 *". 30th of February */
fun Cron.nextExecution(): LocalDateTime =
    ExecutionTime
        .forCron(this)
        .nextExecution(ZonedDateTime.now())
        .orElseThrow { IllegalStateException("Cron expression '${asString()}' has no future execution") }
        .toLocalDateTime()
