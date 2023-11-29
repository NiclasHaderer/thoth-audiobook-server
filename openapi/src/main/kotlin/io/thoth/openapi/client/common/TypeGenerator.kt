package io.thoth.openapi.client.common

import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

typealias GenerateType = (classType: ClassType) -> TypeGenerator.Type

@OptIn(InternalAPI::class)
abstract class TypeGenerator<T : TypeGenerator.Type> {

    abstract class Provider<T: Type, G : TypeGenerator<T>>(
        private val clazz: KClass<G>,
        private val paths: List<String>,
    ) {
        private val tsGenerators: List<G> = run {
            paths.map { Reflections(it) }.flatMap { ref ->
                ref.getSubTypesOf(clazz.java).map { it.kotlin.createInstance() }.toList()
            }
        }

        fun generateTypes(classType: ClassType): Pair<T, List<T>> {
            val generator = tsGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
            val generatedSubTypes = mutableListOf<T>()
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

    interface Type {
        @InternalAPI
        val identifier: String

        @InternalAPI
        val content: String?

        @InternalAPI
        val insertionMode: InsertionMode

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
        final override val content: String,
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

    abstract fun createType(classType: ClassType, generateSubType: GenerateType): T

    abstract fun insertionMode(classType: ClassType): InsertionMode

    abstract fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String?

    abstract fun canGenerate(classType: ClassType): Boolean

    open fun priority(classType: ClassType): Int = 0
}
