package io.thoth.server.common.extensions

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.engine.ApplicationEngineEnvironment
import kotlin.system.exitProcess

fun Application.shutdown(): Nothing {
    environment.monitor.raise(ApplicationStopPreparing, environment)
    if (environment is ApplicationEngineEnvironment) {
        (environment as ApplicationEngineEnvironment).stop()
    } else {
        this.dispose()
    }

    exitProcess(1)
}
