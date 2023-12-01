package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType

abstract class TsGenerator : TypeGenerator<TsGenerator.Type>() {
    interface Type : TypeGenerator.Type {
        val parser: ParseMethod
    }

    class InlineType(content: String, name: String?, override val parser: ParseMethod) :
        TypeGenerator.InlineType(content = content, name = name ?: content), Type

    class ReferenceType(reference: String, content: String, name: String, override val parser: ParseMethod) :
        TypeGenerator.ReferenceType(reference, content, name), Type

    companion object :
        Provider<Type, TsGenerator>(
            TsGenerator::class,
            listOf("io.thoth.openapi.client.typescript"),
        )

    enum class ParseMethod(val methodName: String) {
        BLOB("blob"),
        JSON("json"),
        TEXT("text"),
    }

    abstract fun getParsingMethod(classType: ClassType): ParseMethod

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val reference = generateReference(classType, generateSubType)
        val content = generateContent(classType, generateSubType)
        val insertionMode = getInsertionMode(classType)
        val parser = getParsingMethod(classType)
        val name = getName(classType)
        return when (insertionMode) {
            DataType.PRIMITIVE -> {
                require(reference == null) { "Reference must be null for primitive types" }
                InlineType(content = content, name = name, parser = parser)
            }
            DataType.COMPLEX -> {
                require(reference != null) { "Reference must not be null for complex types" }
                require(name != null) { "Name must not be null for complex types" }
                ReferenceType(reference = reference, content = content, name = name, parser = parser)
            }
        }
    }
}
