package io.thoth.openapi.ktor

@Target(AnnotationTarget.CLASS) annotation class Secured(val name: String)

@Target(AnnotationTarget.CLASS) annotation class NotSecured

@Target(AnnotationTarget.CLASS) annotation class Tagged(val name: String)

@Target(AnnotationTarget.CLASS) annotation class Description(val description: String)

@Target(AnnotationTarget.CLASS) @Repeatable annotation class Summary(val summary: String, val method: String)
