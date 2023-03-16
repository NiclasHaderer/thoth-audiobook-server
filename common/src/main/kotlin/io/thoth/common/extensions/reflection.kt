package io.thoth.common.extensions

import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties

val <T, V> KProperty1<T, V>.optional: Boolean
    get() = returnType.isMarkedNullable

val KClass<*>.fields: List<KProperty1<out Any, *>>
    get() = declaredMemberProperties.toList()

val KType.genericArguments: List<KClass<*>>
    get() = arguments.mapNotNull { it.type?.classifier as? KClass<*> }
