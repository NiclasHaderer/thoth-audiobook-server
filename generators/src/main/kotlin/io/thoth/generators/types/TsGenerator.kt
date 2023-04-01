package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

typealias GenerateType = (classType: ClassType) -> TsGenerator.Type

abstract class TsGenerator {
    class Type
    internal constructor(
        val name: String,
        val content: String,
        val inline: Boolean,
    ) {
        fun reference(): String = if (inline) content else name

        override fun toString(): String = content
    }

    abstract fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String

    fun createType(classType: ClassType, generateSubType: (classType: ClassType) -> Type): Type {
        return Type(
            name = generateName(classType),
            content = generateContent(classType, generateSubType),
            inline = shouldInline(classType),
        )
    }

    abstract fun shouldInline(classType: ClassType): Boolean

    abstract fun generateName(classType: ClassType): String

    abstract fun canGenerate(classType: ClassType): Boolean

    open fun priority(classType: ClassType): Int = 0
}

val tsGenerators: List<TsGenerator> =
    listOf(
        ArrayTsGenerator(),
        DateTsGenerator(),
        InterfaceTsGenerator(),
        NumberTsGenerator(),
        RecordTsGenerator(),
        StringTsGenerator(),
        UUIDTsGenerator(),
        ByteArrayTsGenerator(),
        BooleanTsGenerator(),
    )

fun generateTypes(classType: ClassType): List<TsGenerator.Type> {
    val generator = tsGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
    val generatedSubTypes = mutableListOf<TsGenerator.Type>()
    val type =
        generator.createType(classType) { subType ->
            val generatedSubType = generateTypes(subType)
            generatedSubTypes.addAll(generatedSubType)
            generatedSubType.last()
        }
    generatedSubTypes.add(type)
    return generatedSubTypes
}
