package io.thoth.generators.types

import io.thoth.openapi.schema.ClassType

typealias GenerateType = (classType: ClassType) -> TsGenerator.Type

abstract class TsGenerator {
    class Type
    internal constructor(val name: String, val content: String, val inline: Boolean, val parser: ParseMethod) {
        fun reference(): String = if (inline) content else name

        override fun toString(): String = content
    }

    enum class ParseMethod(val methodName: String) {
        BLOB("blob"),
        JSON("json"),
        TEXT("text"),
    }

    abstract fun generateContent(classType: ClassType, generateSubType: (classType: ClassType) -> Type): String

    abstract fun parseMethod(classType: ClassType): ParseMethod

    fun createType(classType: ClassType, generateSubType: (classType: ClassType) -> Type): Type {
        return Type(
            name = generateName(classType),
            content = generateContent(classType, generateSubType),
            inline = shouldInline(classType),
            parser = parseMethod(classType),
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
        BinaryTsGenerator(),
        BooleanTsGenerator(),
        ByteArrayTsGenerator(),
        DateTsGenerator(),
        EnumTsGenerator(),
        InterfaceTsGenerator(),
        NumberTsGenerator(),
        PairTsGenerator(),
        RecordTsGenerator(),
        RedirectTsGenerator(),
        StringTsGenerator(),
        UUIDTsGenerator(),
    )

fun generateTypes(classType: ClassType): Pair<TsGenerator.Type, MutableList<TsGenerator.Type>> {
    val generator = tsGenerators.filter { it.canGenerate(classType) }.maxBy { it.priority(classType) }
    val generatedSubTypes = mutableListOf<TsGenerator.Type>()
    val type =
        generator.createType(classType) { subType ->
            val (actual, generatedSubType) = generateTypes(subType)
            generatedSubTypes.addAll(generatedSubType)
            actual
        }
    generatedSubTypes.add(type)
    return type to generatedSubTypes
}
