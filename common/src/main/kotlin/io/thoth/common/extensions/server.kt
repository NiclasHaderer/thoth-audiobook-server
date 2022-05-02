package io.thoth.common.extensions

import io.ktor.application.*
import io.ktor.server.engine.*
import kotlin.system.exitProcess

fun Application.shutdown() {
    environment.monitor.raise(ApplicationStopPreparing, environment)
    if (environment is ApplicationEngineEnvironment) {
        (environment as ApplicationEngineEnvironment).stop()
    } else {
        this.dispose()
    }

    exitProcess(1)
}
