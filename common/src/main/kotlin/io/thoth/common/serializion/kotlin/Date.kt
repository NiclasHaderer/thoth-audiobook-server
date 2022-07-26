package io.thoth.common.serializion.kotlin


import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


object DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val epoch = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        encoder.encodeLong(epoch)
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(decoder.decodeLong()), ZoneId.systemDefault())
    }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: LocalDate) =
        encoder.encodeLong(value.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())

    override fun deserialize(decoder: Decoder): LocalDate =
        Instant.ofEpochMilli(decoder.decodeLong()).atZone(ZoneId.systemDefault()).toLocalDate()
}
