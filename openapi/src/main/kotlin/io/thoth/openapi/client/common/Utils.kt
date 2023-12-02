package io.thoth.openapi.client.common

import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.client.typescript.TsGenerator

fun Iterable<KtGenerator.Type>.ktReference(): List<KtGenerator.ReferenceType> =
    this.filterIsInstance<KtGenerator.ReferenceType>()

fun Iterable<TsGenerator.Type>.tsReference(): List<TsGenerator.ReferenceType> =
    this.filterIsInstance<TsGenerator.ReferenceType>()

fun Iterable<KtGenerator.Type>.mappedKtReference(): Map<String, KtGenerator.ReferenceType> {
    return ktReference().associateBy { it.name() }
}

fun Iterable<TsGenerator.Type>.mappedTsReference(): Map<String, TsGenerator.ReferenceType> {
    return tsReference().associateBy { it.name() }
}
