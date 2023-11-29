package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType

class StringKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "String"
    }

    override fun getInsertionMode(classType: ClassType) = DataType.PRIMITIVE

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String = "String"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.isSubclassOf(String::class)
    }
}
