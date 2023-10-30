package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.full.createInstance
import org.reflections.Reflections

abstract class TsGenerator : TypeGenerator() {
    interface Type : TypeGenerator.Type {
        val parser: ParseMethod
    }

    class InlineType(content: String, override val parser: ParseMethod) : TypeGenerator.InlineType(content), Type

    class ReferenceType(identifier: String, content: String, override val parser: ParseMethod) :
        TypeGenerator.ReferenceType(identifier, content), Type

    companion object {
        private val tsGenerators: List<TsGenerator> = run {
            val reflections = Reflections("io.thoth.openapi.client.typescript.types")

            reflections.getSubTypesOf(TsGenerator::class.java).map { it.kotlin }.map { it.createInstance() }.toList()
        }

        fun generateTypes(classType: ClassType): Pair<Type, List<Type>> {
            val generator = tsGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
            val generatedSubTypes = mutableListOf<Type>()
            val type =
                generator.createType(classType) { subType ->
                    val (actual, generatedSubType) = generateTypes(subType)
                    generatedSubTypes.addAll(generatedSubType)
                    actual
                }
            generatedSubTypes.add(type)
            return type to generatedSubTypes
        }
    }

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
