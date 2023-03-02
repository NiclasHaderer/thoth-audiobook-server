package io.thoth.common.utils

class Callable<out T>(defaultFactory: () -> T) {
  val value: T by lazy(defaultFactory)

  operator fun invoke(): T {
    @Suppress("UNCHECKED_CAST") return value
  }
}

fun <T> lazyCallable(defaultFactory: () -> T): Callable<T> {
  return Callable(defaultFactory)
}
