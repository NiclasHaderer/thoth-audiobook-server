package io.github.huiibuh.serializers.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


class CustomLocalDateTimeSerializer : StdSerializer<LocalDateTime>(LocalDateTime::class.java) {
    @Throws(IOException::class)
    override fun serialize(value: LocalDateTime, gen: JsonGenerator, sp: SerializerProvider) {
        val epoch = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        gen.writeString(epoch.toString())
    }
}


class CustomLocalDateTimeDesSerializer : StdDeserializer<LocalDateTime?>(LocalDateTime::class.java) {
    @Throws(IOException::class)
    override fun deserialize(jsonparser: JsonParser, context: DeserializationContext?): LocalDateTime {
        val timestamp = jsonparser.text.toLong()
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
    }
}
