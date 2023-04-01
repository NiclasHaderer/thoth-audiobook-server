package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

class InterfaceTsGenerator : TsGenerator() {
    override fun generateContent(classType: ClassType, generateSubType: GenerateType): String {
        val properties = classType.properties
        val name = generateName(classType)
        val tsProperties =
            properties.map {
                "${it.name}${
                if (it.returnType.isMarkedNullable) {
                    "?"
                } else {
                    ""
                }
            }: ${generateSubType(classType.fromMember(it)).reference()};"
            }
        val interfaceStart = "interface $name {\n"
        val interfaceContent = tsProperties.joinToString("\n") { "  $it" }
        val interfaceEnd = "\n}"

        return interfaceStart + interfaceContent + interfaceEnd
    }

    override fun parseMethod(classType: ClassType): ParseMethod = ParseMethod.JSON

    override fun shouldInline(classType: ClassType): Boolean = false

    override fun generateName(classType: ClassType): String = classType.clazz.simpleName!!

    override fun priority(classType: ClassType): Int = -10

    override fun canGenerate(classType: ClassType): Boolean = true
}
