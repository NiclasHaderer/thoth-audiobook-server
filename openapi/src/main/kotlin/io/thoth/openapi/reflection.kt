package io.thoth.openapi

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

internal val KClass<*>.properties: Collection<KProperty1<out Any, *>>
    get() =
        this.memberProperties.filter {
            // checks if the property is a getter
            it.javaField != null
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
