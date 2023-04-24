package io.thoth.generators.types

import io.thoth.openapi.schema.*

class InterfaceTsGenerator : TsGenerator() {

    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val properties = classType.properties
        val tsProperties =
            properties.map {
                "${it.name}${
                if (it.returnType.isMarkedNullable) {
                    "?"
                } else {
                    ""
                }
            }: ${
                if (classType.isGenericProperty(it)) {
                    "${it.returnType}"
                } else if (classType.isParameterizedProperty(it)) {
                    ""
                } else {
                    generateSubType(classType.forMember(it)).reference()
                }
            };"
            }

        val interfaceStart = "interface ${generateName(classType, false, null)} {\n"

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
                    val typePar = classType.resolveTypeParameter(it)!!
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
