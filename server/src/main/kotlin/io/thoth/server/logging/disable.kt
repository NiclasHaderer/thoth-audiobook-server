package io.thoth.server.logging

import java.util.logging.Level
import java.util.logging.Logger


val J_LOGGER = arrayOf<Logger>(Logger.getLogger("org.jaudiotagger"))

fun disableJAudioTaggerLogs() {
    for (l in J_LOGGER) l.level = Level.SEVERE
}
