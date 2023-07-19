package io.thoth.openapi.common

import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

internal fun Route.fullPath(): String {
    var fullPath = "/${selector}"
    var routeParent = parent
    while (routeParent != null) {
        if (routeParent.selector !is AuthenticationRouteSelector) {
            fullPath = "/${routeParent.selector}$fullPath"
        }
        routeParent = routeParent.parent
    }

    return fullPath.replace("/+".toRegex(), "/")
}

internal val KProperty1<*, *>.nullable: Boolean
    get() = this.returnType.isMarkedNullable

internal val KProperty1<*, *>.clazz: KClass<out Any>?
    get() = this.javaField?.declaringClass?.kotlin

internal val KProperty1<*, *>.optional: Boolean
    get() {
        // Get the constructor of the properties class
        val constructor = clazz?.primaryConstructor ?: return false

        // Get the parameter of the constructor that matches the property
        val parameter = constructor.parameters.find { it.name == this.name } ?: return false
        return parameter.isOptional
    }

val KClass<*>.parent: KClass<*>?
    get() = this.java.enclosingClass?.kotlin
