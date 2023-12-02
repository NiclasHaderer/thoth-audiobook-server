package io.thoth.openapi.client.kotlin

fun List<KtGenerator.Type>.reference(): List<KtGenerator.ReferenceType> =
    this.filterIsInstance<KtGenerator.ReferenceType>()

fun List<KtGenerator.Type>.mappedReference(): Map<String, KtGenerator.ReferenceType> {
    return reference().associateBy { it.name() }
}
