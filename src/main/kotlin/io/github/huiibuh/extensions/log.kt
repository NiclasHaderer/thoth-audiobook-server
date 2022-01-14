package io.github.huiibuh.extensions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal fun Any.classLogger(): Logger {
    return LoggerFactory.getLogger(this::class.java)
}
