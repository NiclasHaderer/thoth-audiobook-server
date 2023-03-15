package io.thoth.openapi.schema

import io.ktor.util.reflect.*
import io.thoth.common.extensions.fields
import io.thoth.common.extensions.genericArguments
import mu.KotlinLogging.logger
import java.lang.reflect.ParameterizedType
import kotlin.reflect.*
import kotlin.reflect.full.starProjectedType


class ClassType(val genericArguments: List<KClass<*>>, val clazz: KClass<*>) {
    private val log = logger { }

    /**
     * This holds a map of the property and the parameterized value of the property So this could be something like
     * this: { someProperty: [A, V] } These generics (A,V) can then be resolved th their actual values using the
     * parameterToValue map
     */
    val parameterizedValues: Map<KProperty1<*, *>, List<KTypeParameter>> by lazy {
        val typeParameterMap = clazz.typeParameters.associateBy { it.starProjectedType }

        clazz.fields
            .filter { member -> member.returnType.arguments.any { typeParameterMap.containsKey(it.type) } }
            .mapNotNull { property ->
                val typeParameters = property.returnType.arguments.mapNotNull { typeParameterMap[it.type] }
                if (typeParameters.isNotEmpty()) property to typeParameters else null
            }
            .toMap()
    }

    /**
     * Same structure as `ClassType.parameterizedValues` but the values are already resolved to the next ClassType,
     * because the property return type is parameterized and has a generic as parameter
     */
    val resolvedParameterizedValue: Map<KProperty1<*, *>, ClassType> by lazy {
        val typeParameterMap = clazz.typeParameters.associateBy { it.starProjectedType }

        clazz.fields
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
    val resolvedGenericValues: Map<KProperty1<*, *>, KClass<*>> by lazy {
        clazz.fields
            .mapNotNull { property ->
                val param = clazz.typeParameters.find { it == property.returnType.classifier } ?: return@mapNotNull null
                property to param
            }
            .toMap()
            .mapValues { entry -> parameterToValue[entry.value]!! }
    }

    /** This resolves the generic parameter K/V/T/... to an actual Kotlin class */
    val parameterToValue by lazy { clazz.typeParameters.zip(genericArguments).toMap() }

    companion object {
        inline fun <reified T> create(): ClassType {
            return ClassType(typeOf<T>().genericArguments, T::class)
        }

        fun create(clazz: KClass<*>): ClassType {
            return ClassType(listOf(), clazz)
        }
    }

    /** Create a new ClassType for a member of the current `ClassType.clazz` */
    @OptIn(ExperimentalStdlibApi::class)
    fun fromMember(property: KProperty1<*, *>): ClassType {
        // The member that is retrieved is one of the generic arguments

        return when (property) {
            in resolvedGenericValues -> {
                val newClazz = resolvedGenericValues[property]!!
                log.warn {
                    "Property ${property.name} is a generic parameter of ${clazz.simpleName}"
                    "If newClazz is generic some values might be missing"
                }
                ClassType(listOf(), newClazz)
            }
            // The return type is a parameterized type which takes one of the generic parameters
            in resolvedParameterizedValue -> {
                resolvedParameterizedValue[property]!!
            }
            // The return type is a non-generic class
            else -> {
                val genericArgs = if (property.returnType.javaType is ParameterizedType) {
                    (property.returnType.javaType as ParameterizedType).actualTypeArguments.map { (it as Class<*>).kotlin }
                } else {
                    listOf()
                }
                ClassType(genericArgs, property.returnType.classifier as KClass<*>)
            }
        }
    }
}
