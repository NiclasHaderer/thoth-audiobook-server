package io.github.huiibuh.api.exceptions

import com.papsign.ktor.openapigen.exceptions.OpenAPIBadContentException
import com.papsign.ktor.openapigen.exceptions.OpenAPINoParserException
import com.papsign.ktor.openapigen.exceptions.OpenAPINoSerializerException
import com.papsign.ktor.openapigen.exceptions.OpenAPIParseException
import com.papsign.ktor.openapigen.exceptions.OpenAPIRequiredFieldException
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.throws
import io.ktor.http.*

fun NormalOpenAPIRoute.withOpenAPIExceptions(routeCallback: NormalOpenAPIRoute.() -> Unit) {
    throws(HttpStatusCode.BadRequest,
           null,
           { ex: OpenAPIBadContentException -> APIBadRequest(ex.message ?: "") }) {
        throws(HttpStatusCode.BadRequest,
               null,
               { ex: OpenAPINoSerializerException -> APIBadRequest(ex.message ?: "") }) {
            throws(HttpStatusCode.BadRequest,
                   null,
                   { ex: OpenAPINoParserException -> APIBadRequest(ex.message ?: "") }) {
                throws(HttpStatusCode.BadRequest,
                       null,
                       { ex: OpenAPIParseException -> APIBadRequest(ex.message ?: "") }) {
                    throws(HttpStatusCode.BadRequest,
                           null,
                           { ex: OpenAPIRequiredFieldException -> APIBadRequest(ex.message) }) {
                        this.routeCallback()
                    }
                }
            }
        }
    }
}
