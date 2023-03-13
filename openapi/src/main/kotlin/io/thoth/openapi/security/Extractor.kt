package io.thoth.openapi.security

import io.thoth.common.extensions.parent
import io.thoth.openapi.Secured
import kotlin.reflect.KClass

fun extractSecured(clazz: KClass<*>): Secured? {
    var current: KClass<*>? = clazz
    while (current != null) {
        val annotation = current.annotations.find { it is Secured } as? Secured
        if (annotation != null) {
            return annotation
        }

        current = current.parent
    }
    return null
}
