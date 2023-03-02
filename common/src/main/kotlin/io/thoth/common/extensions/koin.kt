package io.thoth.common.extensions

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

inline fun <reified T> get(): T {
  return object : KoinComponent {
        val value: T by inject()
      }
      .value
}
