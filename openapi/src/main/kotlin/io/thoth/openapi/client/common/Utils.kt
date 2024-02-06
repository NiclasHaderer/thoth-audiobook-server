package io.thoth.openapi.client.common

import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.client.typescript.TsTypeGenerator

fun Iterable<KtTypeGenerator.KtType>.ktReference(): List<KtTypeGenerator.KtReferenceType> =
    this.filterIsInstance<KtTypeGenerator.KtReferenceType>()

fun Iterable<TsTypeGenerator.Type>.tsReference(): List<TsTypeGenerator.ReferenceType> =
    this.filterIsInstance<TsTypeGenerator.ReferenceType>()

fun Iterable<KtTypeGenerator.KtType>.mappedKtReference(): Map<String, KtTypeGenerator.KtReferenceType> {
    return ktReference().associateBy { it.name() }
}

fun Iterable<TsTypeGenerator.Type>.mappedTsReference(): Map<String, TsTypeGenerator.ReferenceType> {
    return tsReference().associateBy { it.name() }
}
