package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtTypeGenerator
import io.thoth.openapi.common.ClassType
import java.lang.reflect.TypeVariable
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.jvm.jvmErasure

class InterfaceKtGenerator : KtTypeGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map { generateSubType(it) }

        val ktProperties = interfaceProperties(classType, generateSubType)

        val classInterface = buildString {
            val interfaceName =
                generateName(
                    classType = classType,
                    resolveGeneric = false,
                    generateSubType = generateSubType,
                    isImpl = false,
                    includeBounds = true,
                )
            append("interface $interfaceName")
            if (superClasses.isNotEmpty()) {
                append(" : ${superClasses.joinToString(", ") { it.reference() }}")
            }
            append(" {\n")
            ktProperties
                .filter { !it.declaredInSuperclass }
                .map {
                    append("    ")
                    if (it.overwrites) append("override ")
                    append("val ${it.name}: ${it.type.name}")
                    if (it.type.typeArguments.isNotEmpty()) append("<${it.type.typeArguments.joinToString(", ")}>")
                    if (it.nullable) append("?")
                    append("\n")
                }
            append("}")
        }

        val classInterfaceImpl = buildString {
            val dataClassName =
                generateName(
                    classType = classType,
                    resolveGeneric = false,
                    generateSubType = generateSubType,
                    isImpl = true,
                    includeBounds = true,
                )
            append("data class $dataClassName(\n")
            ktProperties.mapIndexed { i, it ->
                val propClassType = classType.forMember(it.underlyingProperty)
                val subType = generateSubType(propClassType) as Type

                append("    override val ${it.name}: ")
                if (it.underlyingProperty.returnType is TypeVariable<*>) {
                    append(it.type.name)
                } else {
                    append(subType.nameImpl())
                }
                if (it.type.typeArguments.isNotEmpty()) {
                    append("<")
                    val memberType = it.underlyingProperty.returnType
                    // Iterate over the type arguments. If the type argument is something like `Map<T, BookModel> we
                    // do not have to resolve the first type argument, but we have to resolve the second one and make
                    // it an Impl reference
                    memberType.arguments.map {
                        // This is the case if we have a mix of generic and inline generics
                        // e.g., interface Something<T> { val hello: Map<String, T> }
                        val typeName = if (it.type!!.classifier is KClass<*>) {
                            // This gets called for the inline generics (String) in the example above
                            (generateSubType(ClassType.create(it.type!!)) as Type).referenceImpl()
                        } else {
                            // This gets called for the generics (T) in the example above
                            it.type.toString()
                        }
                        append(typeName)
                    }
                    append(">")
                }
                if (it.nullable) append("?")
                if (i < ktProperties.size - 1) append(",\n")
            }
            append("\n")
            val superInterfaceName =
                generateName(
                    classType = classType,
                    resolveGeneric = false,
                    generateSubType = generateSubType,
                    isImpl = false,
                    includeBounds = false,
                )
            append(") : $superInterfaceName")
        }

        return classInterface + "\n\n" + classInterfaceImpl
    }

    override fun getName(classType: ClassType): String = classType.simpleName

    override fun getInsertionMode(classType: ClassType): DataType {
        return DataType.COMPLEX
    }

    private fun generateName(
        classType: ClassType,
        resolveGeneric: Boolean,
        generateSubType: GenerateType,
        isImpl: Boolean,
        includeBounds: Boolean
    ): String {
        val typeParams = classType.typeParameters()
        val typeParamsString =
            typeParams
                .joinToString(", ") {
                    if (resolveGeneric) {
                        val typePar = classType.resolveTypeParameter(it)
                        val subType = generateSubType.invoke(typePar) as Type
                        if (isImpl) subType.referenceImpl() else subType.reference()
                    } else {
                        // Check if the generic type has upper bounds
                        val upperBounds = it.upperBounds
                        val bounds =
                            if (upperBounds.isNotEmpty()) {
                                upperBounds
                                    .filter { bound ->
                                        val clazz = bound.classifier as KClass<*>
                                        (clazz == Any::class && bound.isMarkedNullable).not()
                                    }
                                    .joinToString(", ") { bound ->
                                        val subtype = generateSubType(ClassType.create(bound)) as Type
                                        if (isImpl) subtype.referenceImpl() else subtype.reference()
                                    }
                            } else {
                                ""
                            }

                        "${it.name} ${
                            if (bounds.isNotEmpty() && includeBounds) {
                                ": $bounds"
                            } else {
                                ""
                            }
                        }"
                    }
                }
                .trim()

        return "${classType.simpleName + if (isImpl) "Impl" else ""}${
            if (typeParams.isNotEmpty()) {
                "<$typeParamsString>"
            } else {
                ""
            }
        }"
    }

    override fun withImports(classType: ClassType, generateSubType: GenerateType): List<String> {
        return classType.memberProperties
            .flatMap {
                // If the property is a generic type, we can skip it
                if (classType.isGenericProperty(it)) emptyList()
                else {
                    val type = generateSubType(classType.forMember(it)) as Type
                    type.imports
                }
            }
            .distinct()
    }

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String {
        return generateName(
            classType = classType,
            resolveGeneric = true,
            generateSubType = generateSubType,
            isImpl = false,
            includeBounds = false,
        )
    }

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true

    override fun generateImplReference(classType: ClassType, generateSubType: GenerateType): String = generateName(
        classType = classType,
        resolveGeneric = true,
        generateSubType = generateSubType,
        isImpl = true,
        includeBounds = false,
    )

    override fun getImplName(classType: ClassType): String = classType.simpleName + "Impl"
}
