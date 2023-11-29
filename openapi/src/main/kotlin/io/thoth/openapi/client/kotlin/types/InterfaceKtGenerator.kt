package io.thoth.openapi.client.kotlin.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.kotlin.KtGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KClass

class InterfaceKtGenerator : KtGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val properties = classType.properties
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map { generateSubType(it) }

        val tsProperties =
            properties.map { property ->
                "val ${property.name}: ${
                    if (classType.isGenericProperty(property)) {
                        // type: T
                        "${property.returnType}"
                    } else if (classType.isParameterizedProperty(property)) {
                        val parameterizedType = generateSubType(classType.forMember(property))
                        parameterizedType.reference()
                    } else {
                        val subType = generateSubType(classType.forMember(property))
                        subType.reference()
                    }
                }${
                    if (property.returnType.isMarkedNullable) {
                        "?"
                    } else {
                        ""
                    }
                }"
            }

        val interfaceStart =
            "interface ${generateName(classType, false, generateSubType)} ${
                if (superClasses.isNotEmpty()) {
                    ": ${superClasses.joinToString(", ") { it.reference() }} "
                } else {
                    ""
                }
            } {\n"

        val interfaceContent = tsProperties.joinToString("\n") { "  $it" }
        val interfaceEnd = "\n}"

        return interfaceStart + interfaceContent + interfaceEnd
    }

    override fun insertionMode(classType: ClassType): InsertionMode {
        return InsertionMode.REFERENCE
    }

    private fun generateName(classType: ClassType, resolveGeneric: Boolean, generateSubType: GenerateType): String {
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
                                            "Any"
                                        } else {
                                            generateSubType(ClassType.create(bound)).reference()
                                        }
                                    }
                            } else {
                                ""
                            }

                        "${it.name} ${
                            if (bounds.isNotEmpty()) {
                                ": $bounds"
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

    override fun generateIdentifier(classType: ClassType, generateSubType: GenerateType): String {
        return generateName(classType, true, generateSubType)
    }

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true
}
