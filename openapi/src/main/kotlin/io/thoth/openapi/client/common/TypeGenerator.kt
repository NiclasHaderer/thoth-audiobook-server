package io.thoth.openapi.client.common

import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.reflections.Reflections

typealias GenerateType = (classType: ClassType) -> TypeGenerator.Type

@OptIn(InternalAPI::class)
abstract class TypeGenerator<T : TypeGenerator.Type> {

    class Provider<T : Type, G : TypeGenerator<T>>(
        private val clazz: KClass<G>,
        private val paths: List<String>,
    ) {
        private val tsGenerators: List<G> = run {
            paths
                .map { Reflections(it) }
                .flatMap { ref -> ref.getSubTypesOf(clazz.java).map { it.kotlin.createInstance() }.toList() }
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
        @InternalAPI val reference: String?

        @InternalAPI val name: String

        @InternalAPI val content: String

        @InternalAPI val dataType: DataType

        fun reference(): String {
            return when (dataType) {
                DataType.PRIMITIVE -> content
                DataType.COMPLEX -> reference ?: "Reference not found for complex type"
            }
        }

        fun name(): String = name
    }

    abstract class InlineType(final override val content: String, override val name: String) : Type {
        override val dataType: DataType = DataType.PRIMITIVE
        override val reference: String = content
    }

    abstract class ReferenceType(
        override val reference: String,
        override val content: String,
        override val name: String
    ) : Type {
        override val dataType: DataType = DataType.COMPLEX

        fun content(): String = content
    }

    enum class DataType {
        PRIMITIVE,
        COMPLEX,
    }

    abstract fun generateContent(classType: ClassType, generateSubType: GenerateType): String

    abstract fun createType(classType: ClassType, generateSubType: GenerateType): T

    abstract fun getInsertionMode(classType: ClassType): DataType

    abstract fun generateReference(classType: ClassType, generateSubType: GenerateType): String?

    abstract fun canGenerate(classType: ClassType): Boolean

    abstract fun getName(classType: ClassType): String?

    open fun priority(classType: ClassType): Int = 0
}
