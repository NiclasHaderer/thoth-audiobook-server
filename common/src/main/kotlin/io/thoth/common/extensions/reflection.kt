package io.thoth.common.extensions

import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.starProjectedType

val <T, V> KProperty1<T, V>.optional: Boolean
    get() = returnType.isMarkedNullable

val KClass<*>.fields: List<KProperty1<out Any, *>>
    get() = declaredMemberProperties.toList()

val KClass<*>.parameterizedValues: Map<KProperty1<*, *>, List<KTypeParameter>>
    get() {
        val typeParameterMap = typeParameters.associateBy { it.starProjectedType }

        return declaredMemberProperties
            .filter { member -> member.returnType.arguments.any { typeParameterMap.containsKey(it.type) } }
            .mapNotNull { property ->
                val typeParameters = property.returnType.arguments.mapNotNull { typeParameterMap[it.type] }
                if (typeParameters.isNotEmpty()) property to typeParameters else null
            }
            .toMap()
    }

val KClass<*>.genericMembers: List<KProperty1<*, *>>
    get() {
        val typeParameterMap = typeParameters.map { it.starProjectedType }.toSet()
        return declaredMemberProperties.filter { member ->
            member.returnType.arguments.any { typeParameterMap.contains(it.type) }
        }
    }

val KType.genericArguments: List<KClass<*>>
    get() = arguments.mapNotNull { it.type?.classifier as? KClass<*> }

class ClassType(val genericArguments: List<KClass<*>>, val clazz: KClass<*>) {

    /**
     * This holds a map of the property and the parameterized value of the property So this could be something like
     * this: { someProperty: [A, V] } These generics (A,V) can then be resolved th their actual values using the
     * parameterToValue map
     */
    val parameterizedValues by lazy { clazz.parameterizedValues }

    /** This is a list with the property of the class as key and the value (of the generic) as a value */
    val resolvedGenericValues: Map<KProperty1<*, *>, KClass<*>> by lazy {
        clazz.declaredMemberProperties
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
    fun fromMember(property: KProperty1<*, *>): ClassType {
        // The member that is retrieved is one of the generic arguments
        return when (property) {
            in resolvedGenericValues -> {
                ClassType(listOf(), resolvedGenericValues[property]!!)
            }
            // The return type is a parameterized type which takes one of the generic parameters
            in parameterizedValues -> {
                ClassType(
                    parameterizedValues[property]!!.mapNotNull { parameterToValue[it] },
                    property.returnType.classifier as KClass<*>,
                )
            }
            // The return type is a non-generic class
            else -> {
                ClassType(listOf(), property.returnType.classifier as KClass<*>)
            }
        }
    }
}
