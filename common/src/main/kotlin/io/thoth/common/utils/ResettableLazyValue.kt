package io.thoth.common.utils

data class ResettableLazyValue<T>(private val compute: () -> T) {
  private var valid = false
  private var value: T? = null

  fun invalidate() {
    valid = false
  }

  operator fun invoke(): T {
    if (!valid) {
      value = compute()
      valid = true
    }
    return value!!
  }
}
