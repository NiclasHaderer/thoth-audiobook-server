package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType

class StringKtGenerator : KtTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType<KtType>): String {
        return "String"
    }

    override fun getInsertionMode(classType: ClassType) = KtDataType.PRIMITIVE

    override fun getName(classType: ClassType): String? = null

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<KtType>): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(String::class)
    }
}
