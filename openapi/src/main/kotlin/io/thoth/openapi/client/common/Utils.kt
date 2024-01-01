package io.thoth.openapi.client.common

import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.client.typescript.TsTypeGenerator

fun Iterable<KtTypeGenerator.Type>.ktReference(): List<KtTypeGenerator.ReferenceType> =
    this.filterIsInstance<KtTypeGenerator.ReferenceType>()

fun Iterable<TsTypeGenerator.Type>.tsReference(): List<TsTypeGenerator.ReferenceType> =
    this.filterIsInstance<TsTypeGenerator.ReferenceType>()

fun Iterable<KtTypeGenerator.Type>.mappedKtReference(): Map<String, KtTypeGenerator.ReferenceType> {
    return ktReference().associateBy { it.name() }
}

fun Iterable<TsTypeGenerator.Type>.mappedTsReference(): Map<String, TsTypeGenerator.ReferenceType> {
    return tsReference().associateBy { it.name() }
}
