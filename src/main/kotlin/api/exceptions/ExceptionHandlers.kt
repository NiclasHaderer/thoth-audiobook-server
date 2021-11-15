package api.exceptions

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*


fun NormalOpenAPIRoute.withNotFoundHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.NotFound,
           APINotFound("Not found").toModel(),
           { ex: APINotFound -> ex.toModel() }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withBadRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.BadRequest,
           APIBadRequest("User error").toModel(),
           { ex: APIBadRequest -> ex.toModel() }) {
        this.routeCallback()
    }
}

fun NormalOpenAPIRoute.withUnauthorizedRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.Unauthorized,
           APIUnauthorized("Please login").toModel(),
           { ex: APIUnauthorized -> ex.toModel() }) {
        this.routeCallback()
    }
}

fun NormalOpenAPIRoute.withForbiddenRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.Forbidden,
           APIForbidden("You don't have permissions to access this url").toModel(),
           { ex: APIForbidden -> ex.toModel() }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withNotImplementedRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.NotImplemented,
           APINotImplemented("This feature does not exist, ... yet").toModel(),
           { ex: APINotImplemented -> ex.toModel() }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withAllErrorHandlers(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    withNotFoundHandling {
        withBadRequestHandling {
            withUnauthorizedRequestHandling {
                withForbiddenRequestHandling {
                    withNotImplementedRequestHandling(routeCallback)
                }
            }
        }
    }
}

fun NormalOpenAPIRoute.withDefaultErrorHandlers(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    withNotFoundHandling {
        withBadRequestHandling {
            withUnauthorizedRequestHandling {
                withForbiddenRequestHandling(routeCallback)
            }
        }
    }
}
