package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.common.ClassType

class AnyKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        return "Any"
    }

    override fun priority(classType: ClassType): Int = -1

    override fun insertionMode(classType: ClassType) = InsertionMode.INLINE

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String = "Any"

    override fun canGenerate(classType: ClassType): Boolean {
        return classType.clazz == Any::class
    }
}
