package io.thoth.openapi.schema

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.typeOf

class ClassType
private constructor(
    val genericArguments: List<ClassType>,
    @PublishedApi internal val _clazz: KClass<*>,
    val isNullable: Boolean
) {
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
            return ClassType(resolvedArgs, type.classifier as KClass<*>, type.isMarkedNullable)
        }

        inline fun <reified T> create(): ClassType {
            return create(typeOf<T>())
        }

        fun wrap(clazz: KClass<*>, args: List<KClass<*>> = listOf()): ClassType {
            return ClassType(args.map { wrap(it) }, clazz, false)
        }
    }

    @Deprecated("No longer exposed")
    val clazz: KClass<*>
        get() = _clazz

    fun isSubclassOf(vararg clazz: KClass<*>): Boolean {
        return clazz.any { this._clazz.isSubclassOf(it) }
    }

    val memberProperties
        get() = _clazz.declaredMemberProperties

    val properties by lazy {
        memberProperties.filter {
            // checks if the property is a getter
            it.javaField != null || _clazz.java.isInterface
        }
    }

    /**
     * A bit more complex than [resolvedGenericValues] because it also takes into account the case where the generic
     * parameter is used in a parameterized type. Something like interface A<T> { prop: List<T> }
     */
    val resolvedParameterizedValue: Map<KProperty1<*, *>, ClassType> by lazy {
        val typeParameterMap = _clazz.typeParameters.associateBy { it.starProjectedType }

        properties
            // Check if any of the parameters take a generic argument as a parameter for their type
            .filter { member -> member.returnType.arguments.any { typeParameterMap.containsKey(it.type) } }
            .mapNotNull { property ->
                // Get the generic arguments for the parameterized type
                val typeParameters =
                    property.returnType.arguments.mapNotNull {
                        // interface A<T> { prop: Map<String, T> }
                        // We are looking at a generic argument, so we need to resolve it
                        // This would be the case for the T part of the Map<String, T>
                        if (typeParameterMap.containsKey(it.type)) {
                            val t = typeParameterMap[it.type]!!
                            genericParamToValue[t]
                        } else {
                            // This is a normal type or an inlined generic argument, so we can just
                            // create a ClassType for it
                            // This would be the case for the String part of the Map<String, T>
                            it.type?.run { create(this) }
                        }
                    }

                // If the type parameters are not empty, then we can resolve the generic arguments
                if (typeParameters.isNotEmpty()) property to typeParameters else null
            }
            .associate { (property, genericArguments) ->
                property to
                    ClassType(
                        genericArguments,
                        property.returnType.classifier as KClass<*>,
                        property.returnType.isMarkedNullable,
                    )
            }
    }

    /** It is a simple generic property. Something like interface A<T> { prop: T } */
    val resolvedGenericValues: Map<KProperty1<*, *>, ClassType> by lazy {
        properties
            .mapNotNull { property ->
                val param =
                    _clazz.typeParameters.find { it == property.returnType.classifier } ?: return@mapNotNull null
                property to genericParamToValue[param]!!
            }
            .toMap()
    }

    /** This resolves the generic parameter K/V/T/... to an actual Kotlin class */
    val genericParamToValue by lazy { _clazz.typeParameters.zip(genericArguments).toMap() }

    /** Create a new ClassType for a member of the current `ClassType.clazz` */
    @OptIn(ExperimentalStdlibApi::class)
    fun forMember(property: KProperty1<*, *>): ClassType {
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

inline fun <reified T : Annotation> ClassType.findAnnotation(): T? {
    return this._clazz.findAnnotation<T>()
}

inline fun <reified T : Annotation> ClassType.findAnnotations(): List<T> {
    return this._clazz.findAnnotations<T>()
}

fun ClassType.isUnit() = this._clazz == Unit::class

fun ClassType.isEnum() = this._clazz.java.enumConstants != null

fun ClassType.enumValues(): Array<*>? = this._clazz.java.enumConstants

val ClassType.simpleName: String
    get() = this._clazz.simpleName ?: this._clazz.qualifiedName ?: this._clazz.java.name

fun ClassType.isGeneric(): Boolean {
    return this._clazz.typeParameters.isNotEmpty()
}

fun ClassType.typeParameters(): List<KTypeParameter> {
    return this._clazz.typeParameters
}

fun ClassType.resolveTypeParameter(typeParameter: KTypeParameter): ClassType? {
    return this.genericParamToValue[typeParameter]
}

fun ClassType.isGenericProperty(prop: KProperty1<*, *>): Boolean {
    return prop in this.resolvedGenericValues
}

fun ClassType.isParameterizedProperty(prop: KProperty1<*, *>): Boolean {
    return prop in this.resolvedParameterizedValue
}
