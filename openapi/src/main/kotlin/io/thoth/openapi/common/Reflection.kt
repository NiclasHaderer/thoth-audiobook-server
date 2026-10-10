package io.thoth.openapi.common

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

internal val KProperty1<*, *>.nullable: Boolean
    get() = this.returnType.isMarkedNullable

// Only a Patch may be left out, every other key is always sent
internal val KProperty1<*, *>.isPatch: Boolean
    get() = returnType.classifier == Patch::class

// A Patch is never null itself, whether the client may send null is up to its type argument
internal val KProperty1<*, *>.acceptsNull: Boolean
    get() = if (isPatch) {
        returnType.arguments
            .single()
            .type
            ?.isMarkedNullable ?: true
    } else {
        nullable
    }

internal val KProperty1<*, *>.clazz: KClass<out Any>?
    get() = this.javaField?.declaringClass?.kotlin

internal val KProperty1<*, *>.optional: Boolean
    get() {
        // Get the constructor of the property class
        val constructor = clazz?.primaryConstructor ?: return false

        // Get the parameter of the constructor that matches the property
        val parameter = constructor.parameters.find { it.name == this.name } ?: return false
        return parameter.isOptional
    }

@InternalAPI
val KClass<*>.parent: KClass<*>?
    get() = this.java.enclosingClass?.kotlin
