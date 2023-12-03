package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType

abstract class KtGenerator : TypeGenerator<KtGenerator.Type>() {
    interface Type : TypeGenerator.Type {
        val imports: List<String>
    }

    class InlineType(content: String, name: String?, override val imports: List<String>) :
        TypeGenerator.InlineType(content, name ?: content), Type

    class ReferenceType(identifier: String, content: String, name: String, override val imports: List<String>) :
        TypeGenerator.ReferenceType(identifier, content, name), Type

    open fun withImports(classType: ClassType, generateSubType: GenerateType): List<String> = emptyList()

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val content = generateContent(classType, generateSubType)
        val reference = generateReference(classType, generateSubType)
        val insertionMode = getInsertionMode(classType)
        val name = getName(classType)
        val imports = withImports(classType, generateSubType)
        return when (insertionMode) {
            DataType.PRIMITIVE -> {
                require(reference == null) { "Reference must be null for primitive types" }
                InlineType(content = content, name = name, imports = imports)
            }
            DataType.COMPLEX -> {
                require(reference != null) { "Reference must not be null for complex types" }
                require(name != null) { "Name must not be null for complex types" }
                ReferenceType(identifier = reference, content = content, name = name, imports = imports)
            }
        }
    }
}
