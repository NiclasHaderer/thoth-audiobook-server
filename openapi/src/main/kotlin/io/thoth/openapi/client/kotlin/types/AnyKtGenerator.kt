package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType

class AnyKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Any"
    }

    override fun getName(classType: ClassType): String? = null

    override fun priority(classType: ClassType): Int = -1

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String? = null

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.clazz == Any::class
    }
}