package io.thoth.openapi.common

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "This API is internal in openapi generator and should not be used. It could be removed or changed without notice.",
)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class InternalAPI
