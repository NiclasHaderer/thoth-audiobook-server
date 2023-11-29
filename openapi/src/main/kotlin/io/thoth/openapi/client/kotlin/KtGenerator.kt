package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType

abstract class KtGenerator : TypeGenerator<KtGenerator.Type>() {
    interface Type : TypeGenerator.Type {
        val imports: List<String>
    }

    class InlineType(content: String, override val imports: List<String>) : TypeGenerator.InlineType(content), Type

    class ReferenceType(identifier: String, content: String, override val imports: List<String>) :
        TypeGenerator.ReferenceType(identifier, content),
        Type

    open fun withImports(classType: ClassType): List<String> = emptyList()

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val content = generateContent(classType, generateSubType)
        val identifier = generateReference(classType, generateSubType)
        val inlineMode = getInsertionMode(classType)
        val imports = withImports(classType)
        return when (inlineMode) {
            DataType.PRIMITIVE -> InlineType(content, imports)
            DataType.COMPLEX -> ReferenceType(identifier!!, content, imports)
        }
    }

    companion object : Provider<Type, KtGenerator>(
        KtGenerator::class,
        listOf("io.thoth.openapi.client.kotlin"),
    )
}
