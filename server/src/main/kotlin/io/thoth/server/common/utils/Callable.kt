package io.thoth.server.common.utils

class Callable<out T>(
    defaultFactory: () -> T,
) {
    val value: T by lazy(defaultFactory)

    operator fun invoke(): T = value
}

fun <T> lazyCallable(defaultFactory: () -> T): Callable<T> = Callable(defaultFactory)
