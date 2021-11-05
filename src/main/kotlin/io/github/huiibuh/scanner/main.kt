package io.github.huiibuh.scanner

import java.util.logging.Level
import java.util.logging.Logger


val J_LOGGER = arrayOf<Logger>(Logger.getLogger("org.jaudiotagger"))
fun main() {
    for (l in J_LOGGER) l.level = Level.OFF
    val home = System.getProperty("user.home")
    val collection = Collection.fromPath("$home/Desktop/audio")
    println("${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000}mb")
    println(collection)
}
