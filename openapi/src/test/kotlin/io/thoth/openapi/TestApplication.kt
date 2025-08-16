package io.thoth.openapi

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.dataconversion.*
import io.ktor.server.resources.Resources
import io.ktor.server.routing.*
import io.thoth.openapi.ktor.Summary
import io.thoth.openapi.ktor.get
import io.thoth.openapi.ktor.plugins.OpenAPIRouting
import io.thoth.openapi.ktor.post
import io.thoth.openapi.serializion.kotlin.UUID_S
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Resource("{path}")
class Routes(
    val path: UUID_S,
    val name: String,
    val someParam: List<Int>,
) {
    @Summary(summary = "V1", method = "GET")
    @Resource("V1")
    class V1(
        val parent: Routes,
    )

    @Summary(summary = "V2", method = "GET")
    @Resource("V2")
    class V2(
        val parent: Routes,
    )

    @Summary(summary = "V3", method = "GET")
    @Resource("V3")
    class V3(
        val parent: Routes,
    )

    @Summary(summary = "V4", method = "GET")
    @Resource("V4")
    class V4(
        val parent: Routes,
    )

    @Summary(summary = "V5", method = "POST")
    @Resource("V5")
    class V5(
        val parent: Routes,
    )
}

interface Something {
    val name: String
}

class ListRoute(
    override val name: String,
    val someParam: List<UUID>,
) : Something

class MapRoute(
    val name: Boolean,
    val someParam: Map<String, UUID>,
)

class SetRoute(
    val name: Float,
    val someParam: Set<UUID>,
)

class GenericRoute<T>(
    val name: Pair<Any, UUID>,
    val someParam: T,
)

class GenericRoute2<T, U>(
    val name: String,
    val someParam: T,
    val someParam2: U,
)

class GenericRoute3<T>(
    val name: String,
    val someParam: Map<String, T>,
    val someParam2: GenericRoute2<T, String>,
)

fun Application.testRoutes() {
    install(Routing)
    install(DataConversion)
    install(Resources)
    install(ContentNegotiation)
    install(OpenAPIRouting)

    routing {
        get<Routes.V1, ListRoute> { TODO() }
        get<Routes.V2, MapRoute> { TODO() }
        get<Routes.V3, SetRoute> { TODO() }
        get<Routes.V4, GenericRoute<UUID>> { TODO() }
        post<Routes.V5, GenericRoute2<LocalDate, UUID>, GenericRoute3<LocalDateTime>> { _, _ -> TODO() }
    }
}
