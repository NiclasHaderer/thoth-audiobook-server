package io.github.huiibuh.api.exceptions

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*


fun NormalOpenAPIRoute.withNotFoundHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.NotFound,
           APINotFound("Not found"),
           { ex: APINotFound -> ex }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withBadRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.BadRequest,
           APIBadRequest("User error"),
           { ex: APIBadRequest -> ex }) {
        this.routeCallback()
    }
}

fun NormalOpenAPIRoute.withUnauthorizedRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.Unauthorized,
           APIUnauthorized("Please login"),
           { ex: APIUnauthorized -> ex }) {
        this.routeCallback()
    }
}

fun NormalOpenAPIRoute.withForbiddenRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.Forbidden,
           APIForbidden("You don't have permissions to access this url"),
           { ex: APIForbidden -> ex }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withNotImplementedRequestHandling(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.NotImplemented,
           APINotImplemented("This feature does not exist, ... yet"),
           { ex: APINotImplemented -> ex }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withInternalError(routeCallback: NormalOpenAPIRoute.() -> Unit) {

    val format = fun(ex: Exception) = APIInternalError(ex.message ?: "", ex.stackTrace)

    throws(HttpStatusCode.InternalServerError,
           format(Exception("There was in internal error")),
           { ex: Exception -> format(ex) }) {
        this.routeCallback()
    }
}


fun NormalOpenAPIRoute.withDefaultErrorHandlers(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    withInternalError {
        withOpenAPIExceptions {
            withNotFoundHandling {
                withBadRequestHandling {
                    withUnauthorizedRequestHandling {
                        withNotImplementedRequestHandling {
                            withForbiddenRequestHandling(routeCallback)
                        }
                    }
                }
            }
        }
    }
}
