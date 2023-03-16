package io.thoth.openapi.schema

import io.thoth.openapi.responses.BinaryResponse
import io.thoth.openapi.responses.FileResponse
import io.thoth.openapi.responses.RedirectResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

object ContentTypeLookup {
    fun forClassType(classType: ClassType): String {
        if (classType.isEnum) return "text/plain"
        when (classType.clazz) {
            // Binary
            BinaryResponse::class -> return "application/octet-stream"
            ByteArray::class -> return "application/octet-stream"
            FileResponse::class -> return "application/octet-stream"
            // Redirect
            RedirectResponse::class -> return "text/plain"
            // Primitives
            String::class -> return "text/plain"
            Int::class -> return "text/plain"
            Long::class -> return "text/plain"
            Double::class -> return "text/plain"
            Float::class -> return "text/plain"
            Boolean::class -> return "text/plain"
            ULong::class -> return "text/plain"
            List::class -> return "text/plain"
            Date::class -> return "text/plain"
            LocalDate::class -> return "text/plain"
            LocalDateTime::class -> return "text/plain"
            BigDecimal::class -> return "text/plain"
            UUID::class -> return "text/plain"
            // Complex
            Map::class -> return "application/json"
            Unit::class -> return "application/json"
            else -> return "application/json"
        }
    }
}
