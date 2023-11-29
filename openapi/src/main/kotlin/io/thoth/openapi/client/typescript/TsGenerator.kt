package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.full.createInstance
import org.reflections.Reflections

abstract class TsGenerator : TypeGenerator<TsGenerator.Type>() {
    interface Type : TypeGenerator.Type {
        val parser: ParseMethod
    }

    class InlineType(content: String, override val parser: ParseMethod) : TypeGenerator.InlineType(content), Type

    class ReferenceType(identifier: String, content: String, override val parser: ParseMethod) :
        TypeGenerator.ReferenceType(identifier, content), Type

    companion object : Provider<Type, TsGenerator>(
        TsGenerator::class,
        listOf("io.thoth.openapi.client.typescript"),
    )

    enum class ParseMethod(val methodName: String) {
        BLOB("blob"),
        JSON("json"),
        TEXT("text"),
    }

    abstract fun parseMethod(classType: ClassType): ParseMethod

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val identifier = generateIdentifier(classType, generateSubType)
        val content = generateContent(classType, generateSubType)
        val insertionMode = insertionMode(classType)
        val parser = parseMethod(classType)
        return when (insertionMode) {
            InsertionMode.INLINE -> InlineType(content, parser)
            InsertionMode.REFERENCE -> ReferenceType(identifier!!, content, parser)
        }
    }
}
