package io.thoth.common.extensions

import io.ktor.server.application.*
import io.ktor.server.engine.*
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
