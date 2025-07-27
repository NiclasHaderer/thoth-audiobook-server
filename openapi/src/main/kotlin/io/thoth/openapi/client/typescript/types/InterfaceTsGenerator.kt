package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsTypeGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KClass

class InterfaceTsGenerator : TsTypeGenerator() {

    override fun generateContent(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map { generateSubType(it) }

        val tsProperties = interfaceProperties(classType, generateSubType)

        return buildString {
            val interfaceName =
                generateName(classType = classType, resolveGeneric = false, generateSubType = generateSubType)
            append("interface $interfaceName")
            if (superClasses.isNotEmpty()) {
                append(" extends ${superClasses.joinToString(", ") { it.reference() }}")
            }
            append(" {\n")
            tsProperties
                .filter { !it.declaredInSuperclass }
                .forEach { property ->
                    append("  ")
                    if (property.overwrites) append("override ")
                    append("${property.name}: ${property.type.name}")
                    if (property.type.typeArguments.isNotEmpty())
                        append("<${property.type.typeArguments.joinToString(", ")}>")
                    if (property.nullable) append(" | undefined")
                    append(";\n")
                }
            append("}")
        }
    }

    override fun getParsingMethod(classType: ClassType): TsParseMethod = TsParseMethod.JSON

    override fun getInsertionMode(classType: ClassType): TsDataType {
        return TsDataType.COMPLEX
    }

    private fun generateName(
        classType: ClassType,
        resolveGeneric: Boolean,
        generateSubType: GenerateType<TsType>,
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
                                    .joinToString(" & ") { bound ->
                                        val clazz = bound.classifier as KClass<*>
                                        if (clazz == Any::class) {
                                            "NonNullable<any>"
                                        } else {
                                            generateSubType(ClassType.create(bound)).reference()
                                        }
                                    }
                            } else {
                                ""
                            }

                        "${it.name} ${
                            if (bounds.isNotEmpty()) {
                                "extends $bounds"
                            } else {
                                ""
                            }
                        }"
                    }
                }
                .trim()

        return "${classType.simpleName}${
            if (typeParams.isNotEmpty()) {
                "<$typeParamsString>"
            } else {
                ""
            }
        }"
    }

    override fun generateReference(classType: ClassType, generateSubType: GenerateType<TsType>): String {
        return generateName(classType, true, generateSubType)
    }

    override fun getName(classType: ClassType): String = classType.simpleName

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true
}
