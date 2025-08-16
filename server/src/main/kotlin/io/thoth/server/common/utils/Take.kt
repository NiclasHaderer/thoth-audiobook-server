package io.thoth.server.common.utils

fun <T> T.take(
    take: Boolean,
    that: T?,
): T =
    if (take) {
        @Suppress("UNCHECKED_CAST")
        (this ?: that) as T
    } else {
        that ?: this
    }
