package io.thoth.common.extensions

import java.time.Duration

fun Duration.toHumanReadable(): String {
    val years = this.toDays() / 365
    val days = this.toDays() % 365
    val hours = this.toHours() % 24
    val minutes = this.toMinutes() % 60
    val seconds = this.seconds % 60

    val sb = StringBuilder()
    if (years > 0) {
        sb.append("$years years ")
    }
    if (days > 0) {
        sb.append("$days days ")
    }
    if (hours > 0) {
        sb.append("$hours hours ")
    }
    if (minutes > 0) {
        sb.append("$minutes minutes ")
    }
    if (seconds > 0) {
        sb.append("$seconds seconds ")
    } else if (years == 0L && days == 0L && hours == 0L && minutes == 0L) {
        sb.append("less than a second")
    }
    return sb.toString().trim()
}
