package io.thoth.server.common.extensions

import org.koin.java.KoinJavaComponent

inline fun <reified T : Any> get(): T {
    return KoinJavaComponent.getKoin().inject<T>().value
}
