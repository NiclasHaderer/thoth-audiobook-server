package io.thoth.openapi.client.typescript.types

import io.thoth.openapi.client.common.GenerateType
import io.thoth.openapi.client.typescript.TsGenerator
import io.thoth.openapi.common.ClassType
import kotlin.reflect.KClass
import kotlin.reflect.KTypeParameter

class InterfaceTsGenerator : TsGenerator() {

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val properties = classType.properties
        val superClasses =
            classType.superClasses
                .filter { it.memberProperties.isNotEmpty() }
                .filterNot { it.clazz == Enum::class }
                .map { generateSubType(it) }

        val tsProperties =
            properties.map { property ->
                "${property.name}${
                    if (property.returnType.isMarkedNullable) {
                        "?"
                    } else {
                        ""
                    }
                }: ${
                    if (classType.isGenericProperty(property)) {
                        "${property.returnType}"
                    } else if (classType.isParameterizedProperty(property)) {
                        val typeArgs = property.returnType.arguments.map {
                            val argClassifier = it.type!!.classifier
                            if (argClassifier is KTypeParameter) {
                                argClassifier.name
                            } else {
                                generateSubType(ClassType.create(it.type!!)).reference()
                            }
                        }
                        val parameterizedType = generateSubType(classType.forMember(property))

                        "${parameterizedType.name()}<${typeArgs.joinToString(", ")}>"
                    } else {
                        generateSubType(classType.forMember(property)).reference()
                    }
                };"
            }

        val interfaceStart =
            "interface ${generateName(classType, false, generateSubType)} ${
                if (superClasses.isNotEmpty()) {
                    "extends ${superClasses.joinToString(", ") { it.reference() }} "
                } else {
                    ""
                }
            } {\n"

        val interfaceContent = tsProperties.joinToString("\n") { "  $it" }
        val interfaceEnd = "\n}"

        return interfaceStart + interfaceContent + interfaceEnd
    }

    override fun getParsingMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun getInsertionMode(classType: ClassType): DataType {
        return DataType.COMPLEX
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

    override fun generateReference(classType: ClassType, generateSubType: GenerateType): String {
        return generateName(classType, true, generateSubType)
    }

    override fun getName(classType: ClassType): String = classType.simpleName

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true
}