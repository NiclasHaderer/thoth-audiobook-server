package io.thoth.openapi

import io.thoth.openapi.schema.parent
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

@Target(AnnotationTarget.CLASS) annotation class Secured(val name: String)

@Target(AnnotationTarget.CLASS) annotation class Tagged(val name: String)

@Target(AnnotationTarget.CLASS) annotation class Description(val description: String)

@Target(AnnotationTarget.CLASS) annotation class Summary(val summary: String)

inline fun <reified T : Annotation> KClass<*>.findAnnotationsFirstUp(): List<T> {
    var root: KClass<*>? = this
    var annotations = this.findAnnotations<T>()
    while (root != null && annotations.isEmpty()) {
        annotations = root.findAnnotations()
        root = root.parent
    }

    return annotations
}

inline fun <reified T : Annotation> KClass<*>.findAnnotationsUp(): List<T> {
    var root: KClass<*>? = this
    var annotations = this.findAnnotations<T>()
    while (root != null) {
        annotations = root.findAnnotations()
        root = root.parent
    }

    return annotations
}

inline fun <reified T : Annotation> KClass<*>.findAnnotationUp(): T? {
    return findAnnotationsFirstUp<T>().firstOrNull()
}
