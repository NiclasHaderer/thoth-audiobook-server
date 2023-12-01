package io.thoth.openapi.common

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
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
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

    @OptIn(InternalAPI::class)
    val parent: ClassType?
        get() {
            // Get the parent class
            val parentType = type.jvmErasure.parent ?: return null

            // Get the number of generic arguments the generic class has
            val argCount = parentType.typeParameters.size

            // Because we cannot determine the type of the generic arguments we just use Any instead
            val parentTypeArgs = List(argCount) { KTypeProjection.invariant(Any::class.createType()) }

            return create(parentType.createType(parentTypeArgs))
        }

    val superClasses: List<ClassType>
        get() {
            return clazz.superclasses
                .filterNot { it == Any::class }
                .map {
                    val typeArgs = it.typeParameters.map { KTypeProjection.invariant(Any::class.createType()) }
                    create(it.createType(typeArgs))
                }
        }

    fun isEnum() = this.clazz.java.enumConstants != null

    fun enumValues(): Array<*>? = this.clazz.java.enumConstants

    val simpleName: String
        get() = this.clazz.simpleName ?: this.clazz.qualifiedName ?: this.clazz.java.name

    fun typeParameters(): List<KTypeParameter> = this.clazz.typeParameters

    val clazz: KClass<*>
        get() = type.classifier as KClass<*>

    inline fun <reified T : Annotation> findAnnotation(): T? = this.clazz.findAnnotation<T>()

    inline fun <reified T : Annotation> findAnnotationsFirstUp(): List<T> {
        return clazz.findAnnotationsFirstUp<T>()
    }

    inline fun <reified T : Annotation> findAnnotationUp(): T? {
        return clazz.findAnnotationUp()
    }

    inline fun <reified T : Annotation> findAnnotations(): List<T> {
        return clazz.findAnnotations<T>()
    }

    fun isSubclassOf(vararg clazz: KClass<*>): Boolean = clazz.any { this.clazz.isSubclassOf(it) }

    val declaredMemberProperties: Collection<KProperty1<*, *>>
        get() = clazz.declaredMemberProperties

    val memberProperties: Collection<KProperty1<*, *>>
        get() = clazz.memberProperties

    val properties: List<KProperty1<*, *>>
        get() {
            return declaredMemberProperties.filter {
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

    /**
     * Checks if the property is a generic property with a type parameter, an example would be: interface Something<T> {
     * val hello: T } In this case the property is fully generic, because the type parameter is the complete type, not
     * only a part of it type: T
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun isGenericProperty(prop: KProperty1<*, *>): Boolean {
        return prop.returnType.javaType is TypeVariable<*>
    }

    /**
     * Checks if the property is a generic property with a type parameter, an example would be: interface Something<T> {
     * val hello: T } This is different from a parameterized property, which is a property with a generic type, an
     * example would be: interface Something<T> { val hello: Map<String, T> } The second example is a parameterized
     * property because it has a generic type (Map<String, T>)
     *
     * ```kotlin
     * if (classType.isParameterizedProperty(property)) {
     *     val typeArgs = property.returnType.arguments.map {
     *     val argClassifier = it.type!!.classifier
     *     if (argClassifier is KTypeParameter) {
     *         // type: List<T>
     *         argClassifier.name
     *     } else {
     *         // type List<LibraryPermissionsModel>
     *         generateSubType(ClassType.create(it.type!!)).reference()
     *     }
     * }
     * ```
     *
     * @param prop The property to check
     */
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
