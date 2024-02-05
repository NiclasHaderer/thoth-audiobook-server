package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI

abstract class KtTypeGenerator : TypeGenerator<KtTypeGenerator.Type>() {
    interface Type : TypeGenerator.Type {
        val imports: List<String>

        @InternalAPI
        val implReference: String

        @InternalAPI
        val implName: String

        @OptIn(InternalAPI::class)
        fun referenceImpl() = implReference

        @OptIn(InternalAPI::class)
        fun nameImpl() = implName
    }

    class InlineType(content: String, name: String?, override val imports: List<String>) :
        TypeGenerator.InlineType(content, name ?: content), Type {
        @InternalAPI
        override val implReference = content
        @InternalAPI
        override val implName = name()

    }

    class ReferenceType(
        identifier: String, content: String, name: String,
        @OptIn(InternalAPI::class)
        override val implReference: String,
        @OptIn(InternalAPI::class)
        override val implName: String,
        override val imports: List<String>
    ) :
        TypeGenerator.ReferenceType(identifier, content, name), Type

    open fun withImports(classType: ClassType, generateSubType: GenerateType): List<String> = emptyList()
    open fun generateImplReference(classType: ClassType, generateSubType: GenerateType): String? = null

    open fun getImplName(classType: ClassType): String? = null

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val content = generateContent(classType, generateSubType)
        val reference = generateReference(classType, generateSubType)
        val insertionMode = getInsertionMode(classType)
        val implReference = generateImplReference(classType, generateSubType)
        val name = getName(classType)
        val implName = getImplName(classType)
        val imports = withImports(classType, generateSubType)
        return when (insertionMode) {
            DataType.PRIMITIVE -> {
                require(reference == null) { "Reference must be null for primitive types" }
                InlineType(content = content, name = name, imports = imports)
            }

            DataType.COMPLEX -> {
                require(reference != null) { "Reference must not be null for complex types" }
                require(name != null) { "Name must not be null for complex types" }
                ReferenceType(
                        identifier = reference,
                        content = content,
                        name = name,
                        imports = imports,
                        implReference = implReference ?: reference,
                        implName = implName ?: name
                )
            }
        }
    }
}
