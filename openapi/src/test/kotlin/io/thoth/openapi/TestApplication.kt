package io.thoth.openapi

import io.ktor.resources.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.Resources
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.plugins.OpenAPIRouting
import io.thoth.openapi.serializion.kotlin.UUID_S
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Resource("{path}") class Routes(val path: UUID_S, val name: String, val someParam: List<Int>)

class ListRoute(val name: String, val someParam: List<UUID>)

class MapRoute(val name: Boolean, val someParam: Map<String, UUID>)

class SetRoute(val name: Float, val someParam: Set<UUID>)

class GenericRoute<T>(val name: Pair<Any, UUID>, val someParam: T)

class GenericRoute2<T, U>(val name: String, val someParam: T, val someParam2: U)

class GenericRoute3<T>(val name: String, val someParam: Map<String, T>, val someParam2: GenericRoute2<T, String>)

fun ApplicationTestBuilder.testRoutes() {
    install(Routing)
    install(DataConversion)
    install(Resources)
    install(ContentNegotiation)
    install(OpenAPIRouting)

    routing {
        get<Routes, ListRoute>("1") { TODO() }
        get<Routes, MapRoute>("2") { TODO() }
        get<Routes, SetRoute>("3") { TODO() }
        get<Routes, GenericRoute<UUID>>("4") { TODO() }
        get<Routes, GenericRoute2<LocalDate, UUID>>("5") { TODO() }
        get<Routes, GenericRoute3<LocalDateTime>>("6") { TODO() }
    }
}
