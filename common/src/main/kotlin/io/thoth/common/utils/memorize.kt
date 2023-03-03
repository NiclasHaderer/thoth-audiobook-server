package io.thoth.common.utils

class Memoize<out R>(val f: () -> R) : () -> R {
    private val cached by lazy { f() }
    override fun invoke(): R = cached
}

class Memoize1<in T, out R>(val f: (T) -> R) : (T) -> R {
    private val values = mutableMapOf<Int, R>()
    override fun invoke(x: T): R {
        return values.getOrPut(x.hashCode()) { f(x) }
    }
}

fun <R> (() -> R).memoize(): () -> R = Memoize(this)

fun <R, T> ((T) -> R).memoize(): (t: T) -> R = Memoize1<T, R>(this)
