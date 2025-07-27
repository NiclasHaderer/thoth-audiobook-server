package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import java.util.*

class UUIDKtGenerator : KtTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<KtType>): String {
        return "UUID"
    }

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun getName(classType: ClassType): String? = null

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<KtType>): String? = null

    override fun withImports(classType: ClassType, generateSubType: GenerateType<KtType>): List<String> {
        return listOf("import java.util.UUID")
    }

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(UUID::class)
    }
}
