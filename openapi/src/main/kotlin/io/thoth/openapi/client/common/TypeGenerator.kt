package io.thoth.openapi.client.common

import io.thoth.openapi.common.ClassType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import org.reflections.Reflections

typealias GenerateType<T> = (classType: ClassType) -> T

data class PropertyType(val name: String, val typeArguments: List<String>)

data class Property(
    val name: String,
    val nullable: Boolean,
    val type: PropertyType,
    val overwrites: Boolean,
    val declaredInSuperclass: Boolean,
    val underlyingProperty: KProperty1<*, *>
)

abstract class TypeGenerator<TYPE, DATA_TYPE> {

    class Provider<TYPE, DATA_TYPE, GENERATOR : TypeGenerator<TYPE, DATA_TYPE>>(
        private val clazz: KClass<GENERATOR>,
        private val paths: List<String>,
    ) {
        private val generators: List<GENERATOR> = run {
            paths
                .map { Reflections(it) }
                .flatMap { ref -> ref.getSubTypesOf(clazz.java).map { it.kotlin.createInstance() }.toList() }
        }

        fun generateTypes(classType: ClassType): Pair<TYPE, List<TYPE>> {
            val generator = generators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
            val generatedSubTypes = mutableListOf<TYPE>()
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

    abstract fun generateContent(classType: ClassType, generateSubType: GenerateType<TYPE>): String

    abstract fun createType(classType: ClassType, generateSubType: GenerateType<TYPE>): TYPE

    abstract fun getInsertionMode(classType: ClassType): DATA_TYPE

    abstract fun generateReference(classType: ClassType, generateSubType: GenerateType<TYPE>): String?

    abstract fun canGenerate(classType: ClassType): Boolean

    abstract fun getName(classType: ClassType): String?

    open fun priority(classType: ClassType): Int = 0
}
