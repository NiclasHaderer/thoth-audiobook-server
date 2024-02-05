package io.thoth.openapi.client.common

import io.thoth.openapi.common.ClassType
import io.thoth.openapi.common.InternalAPI
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createInstance

typealias GenerateType = (classType: ClassType) -> TypeGenerator.Type

data class PropertyType(
    val name: String,
    val typeArguments: List<String>
)

data class Property(
    val name: String,
    val nullable: Boolean,
    val type: PropertyType,
    val overwrites: Boolean,
    val declaredInSuperclass: Boolean,
    val underlyingProperty: KProperty1<*, *>
)

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
        @InternalAPI
        val reference: String?

        @InternalAPI
        val name: String

        @InternalAPI
        val content: String

        @InternalAPI
        val dataType: DataType

        fun reference(): String {
            return when (dataType) {
                DataType.PRIMITIVE -> content
                DataType.COMPLEX -> reference ?: "Reference not found for complex type"
            }
        }

        fun name(): String = name
    }

    abstract class InlineType(final override var content: String, override var name: String) : Type {
        override val dataType: DataType = DataType.PRIMITIVE
        override val reference: String
            get() = content
    }

    abstract class ReferenceType(
        override var reference: String,
        override var content: String,
        override var name: String
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

    fun interfaceProperties(
        classType: ClassType,
        generateSubType: GenerateType
    ): List<Property> {
        val properties = classType.memberProperties

        return properties.map { property ->
            val overwritesProperty = classType.isOverwrittenProperty(property)
            val propertyName = property.name
            val declaredInSuperclass = classType.properties.none { it.name == propertyName }

            val propertyType: PropertyType = run {
                if (classType.isGenericProperty(property)) {
                    // Fully generic property, example: interface Something<T> { val hello: T }
                    PropertyType(
                        name = property.returnType.toString(),
                        typeArguments = emptyList(),
                    )
                } else if (classType.isParameterizedProperty(property)) {
                    // Parameterized property, example: interface Something<T> { val hello: Map<String, T> }
                    val typeArgs = property.returnType.arguments.map {
                        val argClassifier = it.type!!.classifier
                        if (argClassifier is KTypeParameter) {
                            argClassifier.name
                        } else {
                            generateSubType(ClassType.create(it.type!!)).reference()
                        }
                    }
                    val parameterizedType = generateSubType(classType.forMember(property))

                    PropertyType(
                        name = parameterizedType.name(),
                        typeArguments = typeArgs,
                    )
                } else {
                    // Regular property, example: interface Something { val hello: String }
                    PropertyType(
                        name = generateSubType(classType.forMember(property)).name(),
                        typeArguments = emptyList(),
                    )
                }
            }

            val nullable = property.returnType.isMarkedNullable
            Property(
                name = propertyName,
                nullable = nullable,
                type = propertyType,
                overwrites = overwritesProperty,
                declaredInSuperclass = declaredInSuperclass,
                underlyingProperty = property,
            )
        }
    }
}
