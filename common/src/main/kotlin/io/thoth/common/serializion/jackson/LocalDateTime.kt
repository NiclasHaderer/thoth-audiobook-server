package io.thoth.common.serializion.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CustomLocalDateTimeSerializer : StdSerializer<LocalDateTime>(LocalDateTime::class.java) {
  @Throws(IOException::class)
  override fun serialize(value: LocalDateTime, gen: JsonGenerator, sp: SerializerProvider) {
    val epoch = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    gen.writeNumber(epoch)
  }
}

class CustomLocalDateTimeDesSerializer :
    StdDeserializer<LocalDateTime?>(LocalDateTime::class.java) {
  @Throws(IOException::class)
  override fun deserialize(
      jsonparser: JsonParser,
      context: DeserializationContext?
  ): LocalDateTime {
    val timestamp = jsonparser.text.toLong()
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
  }
}

class CustomLocalDateSerializer : StdSerializer<LocalDate>(LocalDate::class.java) {
  @Throws(IOException::class)
  override fun serialize(value: LocalDate, gen: JsonGenerator, sp: SerializerProvider) {
    val epoch = value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    gen.writeNumber(epoch)
  }
}

class CustomLocalDateDesSerializer : StdDeserializer<LocalDate?>(LocalDate::class.java) {
  @Throws(IOException::class)
  override fun deserialize(jsonparser: JsonParser, context: DeserializationContext?): LocalDate {
    val timestamp = jsonparser.text.toLong()
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
  }
}
