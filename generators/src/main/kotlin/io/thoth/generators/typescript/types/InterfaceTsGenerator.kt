package io.thoth.generators.typescript.types

import io.thoth.generators.common.ClassType
import kotlin.reflect.KTypeParameter

class InterfaceTsGenerator : TsGenerator() {

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val properties = classType.properties
        val superClasses = classType.superClasses.map { generateSubType(it) }

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
                            generateSubType(ClassType.create(it.type!!)).name
                        }
                    }
                    val parameterizedType = generateSubType(classType.forMember(property))

                    "${parameterizedType.name}<${typeArgs.joinToString(", ")}>"
                } else {
                    generateSubType(classType.forMember(property)).reference()
                }
            };"
            }

        val interfaceStart =
            "interface ${generateName(classType, false, null)} ${
            if (superClasses.isNotEmpty()) {
                "extends ${superClasses.joinToString(", ") { it.name }} "
            } else {
                ""
            }
        } {\n"

        val interfaceContent = tsProperties.joinToString("\n") { "  $it" }
        val interfaceEnd = "\n}"

        return interfaceStart + interfaceContent + interfaceEnd
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun insertionMode(classType: ClassType): InsertionMode {
        return InsertionMode.REFERENCE
    }

    private fun generateName(classType: ClassType, resolveGeneric: Boolean, generateSubType: GenerateType?): String {
        val typeParams = classType.typeParameters()
        val typeParamsString =
            typeParams.joinToString(", ") {
                if (resolveGeneric) {
                    val typePar = classType.resolveTypeParameter(it)
                    val subType = generateSubType?.invoke(typePar)
                    subType?.reference() ?: "unknown"
                } else {
                    it.name
                }
            }
        return "${classType.simpleName}${
            if (typeParams.isNotEmpty()) {
                "<$typeParamsString>"
            } else {
                ""
            }
        }"
    }

    override fun generateName(classType: ClassType, generateSubType: GenerateType): String {
        return generateName(classType, true, generateSubType)
    }

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true
}
