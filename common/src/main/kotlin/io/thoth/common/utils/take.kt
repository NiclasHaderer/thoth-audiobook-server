package io.thoth.common.utils

fun <T> T.take(take: Boolean, that: T?): T {
    return if (take) {
        @Suppress("UNCHECKED_CAST")
        (this ?: that) as T
    } else {
        that ?: this
    }
}
