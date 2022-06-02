package io.thoth.openapi

@Target(AnnotationTarget.CLASS)
annotation class Path(val path: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class PathParam(val description: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class QueryParam(val description: String)
