package io.thoth.openapi

@Target(AnnotationTarget.CLASS)
annotation class Path(val path: String)

// TODO perhaps return a 404 if the path param does not match.
// TODO Check if you can say that you do not want to handle it perhaps with the proceed() method
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class PathParam(val description: String)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class QueryParam(val description: String)
