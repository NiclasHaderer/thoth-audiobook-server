package io.thoth.openapi.client.kotlin

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.common.Property
import io.thoth.openapi.client.common.PropertyType
import io.thoth.openapi.client.common.TypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KTypeParameter

abstract class KtTypeGenerator : TypeGenerator<KtTypeGenerator.KtType, KtTypeGenerator.KtDataType>() {

    enum class KtDataType {
        PRIMITIVE,
        COMPLEX,
    }

    abstract class KtType {
        protected abstract val name: String
        protected abstract val imports: List<String>
        protected abstract val implReference: String
        protected abstract val implName: String
        protected abstract val reference: String?
        protected abstract val content: String
        protected abstract val dataType: KtDataType

        fun reference(): String {
            return when (dataType) {
                KtDataType.PRIMITIVE -> content
                KtDataType.COMPLEX -> reference ?: "Reference not found for complex type"
            }
        }

        fun referenceImpl() = implReference

        fun name(): String = name

        fun nameImpl() = implName

        fun imports(): List<String> = imports
    }

    class KtInlineType(override val content: String, name: String?, override val imports: List<String>) : KtType() {
        override val name = name ?: content
        override val implReference = content
        override val implName = this.name
        override val reference: String = content
        override val dataType = KtDataType.PRIMITIVE
    }

    class KtReferenceType(
        override val reference: String,
        override val content: String,
        override val name: String,
        override val implReference: String,
        override val implName: String,
        override val imports: List<String>,
    ) : KtType() {
        override val dataType: KtDataType = KtDataType.COMPLEX

        fun content() = content
    }

    open fun withImports(classType: ClassType, generateSubType: GenerateType<KtType>): List<String> = emptyList()

    open fun generateImplReference(classType: ClassType, generateSubType: GenerateType<KtType>): String? = null

    open fun getImplName(classType: ClassType): String? = null

    override fun createType(classType: ClassType, generateSubType: GenerateType<KtType>): KtType {
        val content = generateContent(classType, generateSubType)
        val reference = generateReference(classType, generateSubType)
        val insertionMode = getInsertionMode(classType)
        val implReference = generateImplReference(classType, generateSubType)
        val name = getName(classType)
        val implName = getImplName(classType)
        val imports = withImports(classType, generateSubType)
        return when (insertionMode) {
            KtDataType.PRIMITIVE -> {
                require(reference == null) { "Reference must be null for primitive types" }
                KtInlineType(content = content, name = name, imports = imports)
            }
            KtDataType.COMPLEX -> {
                require(reference != null) { "Reference must not be null for complex types" }
                require(name != null) { "Name must not be null for complex types" }
                KtReferenceType(
                    reference = reference,
                    content = content,
                    name = name,
                    imports = imports,
                    implReference = implReference ?: reference,
                    implName = implName ?: name,
                )
            }
        }
    }

    fun interfaceProperties(
        classType: ClassType,
        generateSubType: GenerateType<KtType>,
        impl: Boolean,
    ): List<Property> {
        val properties = classType.memberProperties

        return properties.map { property ->
            val overwritesProperty = classType.isOverwrittenProperty(property)
            val propertyName = property.name
            val declaredInSuperclass = classType.properties.none { it.name == propertyName }

            val propertyType: PropertyType = run {
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
                                if (impl) subType.referenceImpl() else subType.reference()
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
