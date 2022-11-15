package io.thoth.common.extensions


class IfFalse<T>(
    val bool: Boolean?,
    val value: T?
) {
    fun <V : T?> otherwise(otherwise: V): V = if (bool != null && bool && value != null) value as V else otherwise
}

fun <T> Boolean?.isTrue(value: T?) = IfFalse(bool = this, value = value)
