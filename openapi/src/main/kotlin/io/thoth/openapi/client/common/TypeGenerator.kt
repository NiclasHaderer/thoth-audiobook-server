package io.thoth.openapi.client.common

import io.thoth.openapi.common.ClassType

typealias GenerateType = (classType: ClassType) -> TypeGenerator.Type

abstract class TypeGenerator {
    abstract class Type internal constructor(val name: String, val content: String, val inlineMode: InsertionMode) {
        fun reference(): String = if (inlineMode == InsertionMode.INLINE) content else name

        override fun toString(): String = content
    }

    enum class InsertionMode {
        INLINE,
        REFERENCE,
    }

    abstract fun generateContent(classType: ClassType, generateSubType: GenerateType): String

    abstract fun createType(classType: ClassType, generateSubType: GenerateType): Type

    abstract fun insertionMode(classType: ClassType): InsertionMode

    abstract fun generateName(classType: ClassType, generateSubType: GenerateType): String

    abstract fun canGenerate(classType: ClassType): Boolean

    open fun priority(classType: ClassType): Int = 0
}
