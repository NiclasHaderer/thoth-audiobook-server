package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.full.createInstance
import org.reflections.Reflections

abstract class KtGenerator : TypeGenerator() {
    interface Type : TypeGenerator.Type{
        val imports: List<String>
    }

    class InlineType(content: String, override val imports: List<String>) : TypeGenerator.InlineType(content), Type

    class ReferenceType(identifier: String, content: String, override val imports: List<String>) : TypeGenerator.ReferenceType(identifier, content), Type

    open fun withImports(classType: ClassType): List<String> = emptyList()

    override fun createType(classType: ClassType, generateSubType: GenerateType): Type {
        val content = generateContent(classType, generateSubType)
        val identifier = generateIdentifier(classType, generateSubType)
        val inlineMode = insertionMode(classType)
        val imports = withImports(classType)
        return when (inlineMode) {
            InsertionMode.INLINE -> InlineType(content, imports)
            InsertionMode.REFERENCE -> ReferenceType(identifier!!, content, imports)
        }
    }

    companion object {
        private val tsGenerators: List<KtGenerator> = run {
            val reflections = Reflections("io.thoth.openapi.client.kotlin.types")
            reflections.getSubTypesOf(KtGenerator::class.java).map { it.kotlin }.map { it.createInstance() }.toList()
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
}
