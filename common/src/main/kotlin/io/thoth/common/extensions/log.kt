package io.thoth.common.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Any.classLogger(): Logger {
    return LoggerFactory.getLogger(this::class.java)
}
