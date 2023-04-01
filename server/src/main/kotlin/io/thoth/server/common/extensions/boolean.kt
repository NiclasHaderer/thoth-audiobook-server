package io.thoth.server.common.extensions

class IfFalse<T>(val bool: Boolean?, val value: T?) {
    fun <V : T?> otherwise(otherwise: V): V = if (bool != null && bool && value != null) value as V else otherwise
}

fun <T> Boolean?.isTrue(value: T?) = io.thoth.server.common.extensions.IfFalse(bool = this, value = value)
