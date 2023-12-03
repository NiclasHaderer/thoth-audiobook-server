package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KClass


class InterfaceKtGenerator : KtGenerator() {
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
            ktProperties.filter { !it.declaredInSuperclass }.mapIndexed { i, it ->
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
                append("    ")
                append("override ")
                append("val ${it.name}: ${it.type.name}")
                if (it.type.typeArguments.isNotEmpty()) append("<${it.type.typeArguments.joinToString(", ")}>")
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
                        val subType = generateSubType.invoke(typePar)
                        subType.reference()
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
                                        generateSubType(ClassType.create(bound)).reference()
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
}
