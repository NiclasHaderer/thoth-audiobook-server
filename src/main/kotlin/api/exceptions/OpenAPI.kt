package api.exceptions

import com.papsign.ktor.openapigen.exceptions.*
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*

// OpenAPIBadContentException
// OpenAPINoParserException
// OpenAPINoSerializerException
// OpenAPIParseException
// OpenAPIRequiredFieldException

fun NormalOpenAPIRoute.withOpenAPIExceptions(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.BadRequest,
           OpenAPIBadContentException::class,
           { ex: OpenAPIBadContentException -> APIBadRequest(ex.message ?: "").toModel() }) {
        throws(HttpStatusCode.BadRequest,
               OpenAPINoSerializerException::class,
               { ex: OpenAPINoSerializerException -> APIBadRequest(ex.message ?: "").toModel() }) {
            throws(HttpStatusCode.BadRequest,
                   OpenAPINoParserException::class,
                   { ex: OpenAPINoParserException -> APIBadRequest(ex.message ?: "").toModel() }) {
                throws(HttpStatusCode.BadRequest,
                       OpenAPIParseException::class,
                       { ex: OpenAPIParseException -> APIBadRequest(ex.message ?: "").toModel() }) {
                    throws(HttpStatusCode.BadRequest,
                           OpenAPIRequiredFieldException::class,
                           { ex: OpenAPIRequiredFieldException -> APIBadRequest(ex.message).toModel() }) {
                        this.routeCallback()
                    }
                }
            }
        }
    }
}
