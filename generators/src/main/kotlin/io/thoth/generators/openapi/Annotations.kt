package io.thoth.generators.openapi

@Target(AnnotationTarget.CLASS) annotation class Secured(val name: String)

@Target(AnnotationTarget.CLASS) annotation class Tagged(val name: String)

@Target(AnnotationTarget.CLASS) annotation class Description(val description: String)

@Target(AnnotationTarget.CLASS) @Repeatable annotation class Summary(val summary: String, val method: String)
