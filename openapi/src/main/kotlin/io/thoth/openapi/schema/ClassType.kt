package io.thoth.openapi.schema

import java.lang.reflect.TypeVariable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class ClassType
private constructor(
    @PublishedApi internal val type: KType,
) {
    companion object {
        fun create(type: KType): ClassType {
            return ClassType(type)
        }

        inline fun <reified T> create(): ClassType {
            return create(typeOf<T>())
        }
    }

    val genericArguments: List<ClassType>
        get() {
            return type.arguments.map { create(it.type!!) }
        }

    val isNullable: Boolean
        get() = type.isMarkedNullable

    val parent: ClassType
        get() {
            // Get the parent class
            val parentType = type.jvmErasure.parent?.java?.kotlin!!

            // Get the number of generic arguments the generic class has
            val argCount = parentType.typeParameters.size

            // Because we cannot determine the type of the generic arguments we just use Any instead
            val parentTypeArgs = List(argCount) { KTypeProjection.invariant(Any::class.createType()) }

            return create(parentType.createType(parentTypeArgs))
        }

    fun isEnum() = this.clazz.java.enumConstants != null
    fun enumValues(): Array<*>? = this.clazz.java.enumConstants
    val simpleName: String
        get() = this.clazz.simpleName ?: this.clazz.qualifiedName ?: this.clazz.java.name

    fun typeParameters(): List<KTypeParameter> = this.clazz.typeParameters

    val clazz: KClass<*>
        get() = type.classifier as KClass<*>

    fun isSubclassOf(vararg clazz: KClass<*>): Boolean = clazz.any { this.clazz.isSubclassOf(it) }
    val memberProperties: Collection<KProperty1<*, *>>
        get() = clazz.declaredMemberProperties

    val properties: List<KProperty1<*, *>>
        get() {
            return memberProperties.filter {
                // checks if the property is a getter
                it.javaField != null || this.clazz.java.isInterface
            }
        }

    private val genericArgValueMap: Map<KTypeParameter, KTypeProjection> by lazy {
        val typeParameters = this.clazz.typeParameters
        val typeArgs = this.type.arguments

        typeParameters.zip(typeArgs).toMap()
    }

    fun resolveTypeParameter(typeParameter: KTypeParameter): ClassType {
        assert(typeParameter in type.jvmErasure.typeParameters) {
            "Type parameter $typeParameter is not a type parameter of ${type.jvmErasure}"
        }

        val index = type.jvmErasure.typeParameters.indexOf(typeParameter)
        val typeArg = type.arguments[index]
        return ClassType(typeArg.type!!)
    }

    fun isGenericProperty(prop: KProperty1<*, *>) = prop.returnType is KTypeParameter

    fun isParameterizedProperty(prop: KProperty1<*, *>) = prop.returnType.arguments.isNotEmpty()

    /** Create a new ClassType for a member of the current `ClassType.clazz` */
    @OptIn(ExperimentalStdlibApi::class)
    fun forMember(property: KProperty1<*, *>): ClassType {
        // Get property from clazz
        val member = this.clazz.declaredMemberProperties.first { it.name == property.name }
        val memberType: KType = member.returnType

        // If the member type is a complete generic type we resolve it and return it
        // e.g. interface Something<T> { val hello: T }
        if (memberType.javaType is TypeVariable<*>) {
            val resolved = genericArgValueMap[memberType.classifier]!!
            return create(resolved.type!!)
        }

        val memberParameters =
            memberType.arguments.map {
                // This is the case if we have a mix of generic and inline generics
                // e.g. interface Something<T> { val hello: Map<String, T> }
                if (it.type!!.classifier is KClass<*>) {
                    KTypeProjection(variance = it.variance, type = it.type)
                } else {
                    // This gets called for the generics (T) in the example above
                    genericArgValueMap[it.type!!.classifier as KTypeParameter]!!
                }
            }

        val newType = memberType.jvmErasure.createType(memberParameters)
        return create(newType)
    }
}

val KClass<*>.parent: KClass<*>?
    get() = this.java.enclosingClass?.kotlin

inline fun <reified T : Annotation> ClassType.findAnnotation(): T? {
    return this.clazz.findAnnotation<T>()
}

inline fun <reified T : Annotation> ClassType.findAnnotations(): List<T> {
    return this.clazz.findAnnotations<T>()
}
