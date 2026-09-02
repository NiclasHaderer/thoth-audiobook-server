package io.thoth.auth.interactions

import io.ktor.server.routing.RoutingContext
import io.thoth.auth.thothAuthConfig

interface ThothLogoutParams

fun RoutingContext.logoutUser(
    params: ThothLogoutParams,
    body: Unit,
) {
    call.clearAuthCookies(thothAuthConfig<Any, Any>())
}
