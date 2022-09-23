package io.thoth.common.utils

class Memoize<out R>(val f: () -> R) : () -> R {
    private val cached by lazy { f() }
    override fun invoke(): R = cached
}

fun <R> (() -> R).memoize(): () -> R = Memoize(this)
