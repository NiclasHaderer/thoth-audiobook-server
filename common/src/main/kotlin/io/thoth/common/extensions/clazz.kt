package io.thoth.common.extensions

import kotlin.reflect.KClass

val KClass<*>.parent: KClass<*>?
    get() = this.java.enclosingClass?.kotlin
