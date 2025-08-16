package io.thoth.openapi.client.typescript

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.Property
import io.thoth.openapi.client.common.PropertyType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KTypeParameter

abstract class TsTypeGenerator : TypeGenerator<TsTypeGenerator.TsType, TsTypeGenerator.TsDataType>() {
    enum class TsDataType {
        PRIMITIVE,
        COMPLEX,
    }

    abstract class TsType {
        protected abstract val parser: TsParseMethod
        protected abstract val reference: String?
        protected abstract val name: String
        protected abstract val content: String
        protected abstract val dataType: TsDataType

        fun reference(): String =
            when (dataType) {
                TsDataType.PRIMITIVE -> content
                TsDataType.COMPLEX -> reference ?: "Reference not found for complex type"
            }

        fun name(): String = name

        fun parser(): TsParseMethod = parser
    }

    class TsInlineType(
        override val content: String,
        name: String?,
        override val parser: TsParseMethod,
    ) : TsType() {
        override val name = name ?: content
        override val dataType = TsDataType.PRIMITIVE
        override val reference: String = content
    }

    class TsReferenceType(
        override val reference: String,
        override val content: String,
        override val name: String,
        override val parser: TsParseMethod,
    ) : TsType() {
        override val dataType = TsDataType.COMPLEX

        fun content() = content
    }

    enum class TsParseMethod(
        val methodName: String,
    ) {
        BLOB("blob"),
        JSON("json"),
        TEXT("text"),
    }

    abstract fun getParsingMethod(classType: ClassType): TsParseMethod

    override fun createType(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): TsType {
        val reference = generateReference(classType, generateSubType)
        val content = generateContent(classType, generateSubType)
        val insertionMode = getInsertionMode(classType)
        val parser = getParsingMethod(classType)
        val name = getName(classType)
        return when (insertionMode) {
            TsDataType.PRIMITIVE -> {
                require(reference == null) { "Reference must be null for primitive types" }
                TsInlineType(content = content, name = name, parser = parser)
            }

            TsDataType.COMPLEX -> {
                require(reference != null) { "Reference must not be null for complex types" }
                require(name != null) { "Name must not be null for complex types" }
                TsReferenceType(reference = reference, content = content, name = name, parser = parser)
            }
        }
    }

    fun interfaceProperties(
        classType: ClassType,
        generateSubType: GenerateType<TsType>,
    ): List<Property> {
        val properties = classType.memberProperties

        return properties.map { property ->
            val overwritesProperty = classType.isOverwrittenProperty(property)
            val propertyName = property.name
            val declaredInSuperclass = classType.properties.none { it.name == propertyName }

            val propertyType: PropertyType =
                run {
                    if (classType.isGenericProperty(property)) {
                        // Fully generic property, example: interface Something<T> { val hello: T }
                        PropertyType(name = property.returnType.toString(), typeArguments = emptyList())
                    } else if (classType.isParameterizedProperty(property)) {
                        // Parameterized property, example: interface Something<T> { val hello:
                        // Map<String, T> }
                        val typeArgs =
                            property.returnType.arguments.map {
                                val argClassifier = it.type!!.classifier
                                if (argClassifier is KTypeParameter) {
                                    argClassifier.name
                                } else {
                                    val subType = generateSubType(ClassType.create(it.type!!))
                                    subType.reference()
                                }
                            }
                        val parameterizedType = generateSubType(classType.forMember(property))

                        PropertyType(name = parameterizedType.name(), typeArguments = typeArgs)
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
