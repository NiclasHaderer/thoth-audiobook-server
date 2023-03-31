package io.thoth.openapi.schema

import io.thoth.openapi.properties
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

class ClassType private constructor(val genericArguments: List<ClassType>, val clazz: KClass<*>) {
    val isEnum: Boolean
        get() = clazz.java.enumConstants != null

    companion object {
        private fun resolveArguments(kType: KType): List<ClassType> {
            val classTypes = mutableListOf<ClassType>()
            for (arg in kType.arguments) {
                val type = arg.type
                if (type != null) {
                    classTypes.add(create(type))
                }
            }
            return classTypes
        }

        fun create(type: KType): ClassType {
            val resolvedArgs = resolveArguments(type)
            return ClassType(resolvedArgs, type.classifier as KClass<*>)
        }

        inline fun <reified T> create(): ClassType {
            return create(typeOf<T>())
        }

        fun wrap(clazz: KClass<*>, args: List<KClass<*>> = listOf()): ClassType {
            return ClassType(args.map { wrap(it) }, clazz)
        }
    }

    /**
     * Same structure as `ClassType.parameterizedValues` but the values are already resolved to the next ClassType,
     * because the property return type is parameterized and has a generic as parameter
     */
    val resolvedParameterizedValue: Map<KProperty1<*, *>, ClassType> by lazy {
        val typeParameterMap = clazz.typeParameters.associateBy { it.starProjectedType }

        clazz.properties
            .filter { member -> member.returnType.arguments.any { typeParameterMap.containsKey(it.type) } }
            .mapNotNull { property ->
                val typeParameters = property.returnType.arguments.mapNotNull { typeParameterMap[it.type] }
                if (typeParameters.isNotEmpty()) property to typeParameters.mapNotNull { parameterToValue[it] }
                else null
            }
            .associate { (property, genericArguments) ->
                property to ClassType(genericArguments, property.returnType.classifier as KClass<*>)
            }
    }

    /** This is a list with the property of the class as key and the value (of the generic) as a value */
    val resolvedGenericValues: Map<KProperty1<*, *>, ClassType> by lazy {
        clazz.properties
            .mapNotNull { property ->
                val param = clazz.typeParameters.find { it == property.returnType.classifier } ?: return@mapNotNull null
                property to parameterToValue[param]!!
            }
            .toMap()
    }

    /** This resolves the generic parameter K/V/T/... to an actual Kotlin class */
    val parameterToValue by lazy { clazz.typeParameters.zip(genericArguments).toMap() }

    /** Create a new ClassType for a member of the current `ClassType.clazz` */
    @OptIn(ExperimentalStdlibApi::class)
    fun fromMember(property: KProperty1<*, *>): ClassType {
        // The member that is retrieved is one of the generic arguments

        return when (property) {
            in resolvedGenericValues -> {
                return resolvedGenericValues[property]!!
            }
            // The return type is a parameterized type which takes one of the generic parameters
            in resolvedParameterizedValue -> {
                resolvedParameterizedValue[property]!!
            }
            // The return type is a non-generic class
            else -> {
                val genericArgs =
                    if (property.returnType.javaType is ParameterizedType) {
                        (property.returnType.javaType as ParameterizedType).actualTypeArguments.map {
                            (it as Class<*>).kotlin
                        }
                    } else {
                        listOf()
                    }
                wrap(property.returnType.classifier as KClass<*>, genericArgs)
            }
        }
    }
}

val KClass<*>.parent: KClass<*>?
    get() = this.java.enclosingClass?.kotlin
