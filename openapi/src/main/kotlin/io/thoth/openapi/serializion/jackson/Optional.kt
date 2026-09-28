package io.thoth.openapi.serializion.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.deser.std.ReferenceTypeDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.type.ReferenceType
import java.util.Optional

// Update models tell "not sent" from "sent null": a missing key leaves an Optional empty, a JSON null arrives as a real
// null. Jdk8Module turns that null into an empty Optional as well, and `@JsonSetter(nulls = Nulls.SET)` does not reach
// its deserializer, so this module replaces it. Writing mirrors it: an empty Optional is left out, a null is written.
// Jdk8Module still has to be registered before this one, it is what makes Optional a reference type.
class OptionalModule : SimpleModule() {
    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.addDeserializers(
            object : Deserializers.Base() {
                override fun findReferenceDeserializer(
                    refType: ReferenceType,
                    config: DeserializationConfig,
                    beanDesc: BeanDescription,
                    contentTypeDeserializer: TypeDeserializer?,
                    contentDeserializer: JsonDeserializer<*>?,
                ): JsonDeserializer<*>? =
                    if (refType.hasRawClass(Optional::class.java)) {
                        OptionalDeserializer(refType, contentTypeDeserializer, contentDeserializer)
                    } else {
                        null
                    }
            },
        )
        context.configOverride(Optional::class.java).include =
            JsonInclude.Value.construct(
                JsonInclude.Include.CUSTOM,
                JsonInclude.Include.ALWAYS,
                EmptyOptionalFilter::class.java,
                null,
            )
    }
}

class EmptyOptionalFilter {
    override fun equals(other: Any?): Boolean = other is Optional<*> && other.isEmpty

    override fun hashCode(): Int = 0
}

private class OptionalDeserializer(
    fullType: JavaType,
    typeDeserializer: TypeDeserializer?,
    valueDeserializer: JsonDeserializer<*>?,
) : ReferenceTypeDeserializer<Optional<*>>(fullType, null, typeDeserializer, valueDeserializer) {
    override fun withResolved(
        typeDeserializer: TypeDeserializer?,
        valueDeserializer: JsonDeserializer<*>?,
    ) = OptionalDeserializer(_fullType, typeDeserializer, valueDeserializer)

    override fun getNullValue(ctxt: DeserializationContext): Optional<*>? = null

    override fun getAbsentValue(ctxt: DeserializationContext): Any = Optional.empty<Any>()

    override fun getEmptyValue(ctxt: DeserializationContext): Any = Optional.empty<Any>()

    override fun referenceValue(contents: Any?): Optional<*> = Optional.ofNullable(contents)

    override fun getReferenced(reference: Optional<*>): Any? = reference.orElse(null)

    override fun updateReference(
        reference: Optional<*>,
        contents: Any?,
    ): Optional<*> = Optional.ofNullable(contents)
}
