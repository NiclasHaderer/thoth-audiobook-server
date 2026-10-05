package io.thoth.openapi.serializion.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.introspect.AnnotatedMember
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.thoth.openapi.common.Patch
import kotlin.reflect.full.memberProperties

// A missing key is Patch.Absent and is left out when written. Everything sent arrives as Patch.Set, a JSON null
// included, but only where the type argument is nullable.
class PatchModule : SimpleModule() {
    init {
        addDeserializer(Patch::class.java, PatchDeserializer(null, acceptsNull = true))
        addSerializer(Patch::class.java, PatchSerializer())
    }

    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        // Every other key has to be sent, even for a nullable property: jackson-module-kotlin would otherwise quietly
        // fill a missing one with null, and only a Patch can tell "not sent" apart.
        context.insertAnnotationIntrospector(
            object : NopAnnotationIntrospector() {
                override fun hasRequiredMarker(member: AnnotatedMember): Boolean? =
                    if (member is AnnotatedParameter && member.rawType != Patch::class.java) true else null
            },
        )
        context.configOverride(Patch::class.java).include =
            JsonInclude.Value.construct(JsonInclude.Include.NON_EMPTY, JsonInclude.Include.ALWAYS)
    }
}

private class PatchDeserializer(
    private val value: JsonDeserializer<Any?>?,
    private val acceptsNull: Boolean,
) : JsonDeserializer<Patch<*>>(),
    ContextualDeserializer {
    override fun createContextual(
        ctxt: DeserializationContext,
        property: BeanProperty?,
    ): JsonDeserializer<*> {
        val valueType = ctxt.contextualType.containedTypeOrUnknown(0)
        return PatchDeserializer(ctxt.findContextualValueDeserializer(valueType, property), property.acceptsNull())
    }

    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<*> = Patch.Set(value!!.deserialize(p, ctxt))

    override fun getNullValue(ctxt: DeserializationContext): Patch<*> {
        if (!acceptsNull) ctxt.reportInputMismatch<Unit>(this, "null is not allowed here, leave the key out instead")
        return Patch.Set(null)
    }

    override fun getAbsentValue(ctxt: DeserializationContext): Patch<*> = Patch.Absent

    override fun getEmptyValue(ctxt: DeserializationContext): Patch<*> = Patch.Absent

    // Java generics drop Kotlin's nullability, so it is read off the Kotlin property the value lands in
    private fun BeanProperty?.acceptsNull(): Boolean {
        if (this == null) return true
        val type = member.declaringClass.kotlin.memberProperties
            .find { it.name == name }
            ?.returnType ?: return true
        return type.arguments
            .singleOrNull()
            ?.type
            ?.isMarkedNullable ?: true
    }
}

private class PatchSerializer : StdSerializer<Patch<*>>(Patch::class.java, false) {
    override fun isEmpty(
        provider: SerializerProvider,
        value: Patch<*>,
    ): Boolean = value is Patch.Absent

    override fun serialize(
        value: Patch<*>,
        gen: JsonGenerator,
        provider: SerializerProvider,
    ) = when (value) {
        is Patch.Absent -> gen.writeNull()
        is Patch.Set -> provider.defaultSerializeValue(value.value, gen)
    }
}
