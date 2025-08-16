package io.thoth.openapi.client.common

import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.client.typescript.TsTypeGenerator

fun Iterable<KtTypeGenerator.KtType>.ktReference(): List<KtTypeGenerator.KtReferenceType> =
    this.filterIsInstance<KtTypeGenerator.KtReferenceType>()

fun Iterable<TsTypeGenerator.TsType>.tsReference(): List<TsTypeGenerator.TsReferenceType> =
    this.filterIsInstance<TsTypeGenerator.TsReferenceType>()

fun Iterable<KtTypeGenerator.KtType>.mappedKtReference(): Map<String, KtTypeGenerator.KtReferenceType> =
    ktReference().associateBy {
        it.name()
    }

fun Iterable<TsTypeGenerator.TsType>.mappedTsReference(): Map<String, TsTypeGenerator.TsReferenceType> =
    tsReference().associateBy {
        it.name()
    }
