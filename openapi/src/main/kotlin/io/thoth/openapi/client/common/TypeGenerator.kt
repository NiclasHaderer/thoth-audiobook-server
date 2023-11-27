package io.thoth.openapi.client.common

import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI

typealias GenerateType = (classType: ClassType) -> TypeGenerator.Type

@OptIn(InternalAPI::class)
abstract class TypeGenerator {
    interface Type {
        @InternalAPI val identifier: String
        @InternalAPI val content: String?
        @InternalAPI val insertionMode: InsertionMode

        fun reference(): String {
            return when (insertionMode) {
                InsertionMode.INLINE -> return content ?: error("Content is null, but insertion mode is INLINE")
                InsertionMode.REFERENCE -> identifier
            }
        }

        fun identifier(): String = identifier

        fun content(): String {
            return when (insertionMode) {
                InsertionMode.INLINE -> content ?: error("Content is null, but insertion mode is INLINE")
                InsertionMode.REFERENCE -> content ?: error("Content is null, but insertion mode is REFERENCE")
            }
        }
    }

    abstract class InlineType(
        override val content: String,
    ) : Type {
        override val insertionMode: InsertionMode = InsertionMode.INLINE
        override val identifier: String = content
    }

    abstract class ReferenceType(override val identifier: String, override val content: String) : Type {
        override val insertionMode: InsertionMode = InsertionMode.REFERENCE
    }

    enum class InsertionMode {
        INLINE,
        REFERENCE,
    }

    abstract fun generateContent(classType: ClassType, generateSubType: GenerateType): String

    abstract fun createType(classType: ClassType, generateSubType: GenerateType): Type

    abstract fun insertionMode(classType: ClassType): InsertionMode

    abstract fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String?

    abstract fun canGenerate(classType: ClassType): Boolean

    open fun priority(classType: ClassType): Int = 0
}
