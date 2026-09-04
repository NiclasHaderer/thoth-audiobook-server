@file:Suppress("UnusedReceiverParameter")

package io.thoth.openapi.ktor.responses

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.EntityTagVersion
import io.ktor.http.content.VersionCheckResult
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.RoutingContext

class BinaryResponse(
    val bytes: ByteArray,
    val contentType: ContentType = ContentType.Application.OctetStream,
    val cacheControl: String? = null,
    val etag: String? = null,
) : BaseResponse {
    override suspend fun respond(call: ApplicationCall) {
        if (cacheControl != null) call.response.header(HttpHeaders.CacheControl, cacheControl)
        if (etag != null) {
            call.response.header(HttpHeaders.ETag, "\"$etag\"")
            val check = EntityTagVersion(etag, weak = false).check(call.request.headers)
            if (check != VersionCheckResult.OK) {
                call.respond(check.statusCode)
                return
            }
        }
        call.respondBytes(bytes, contentType)
    }
}

fun RoutingContext.binaryResponse(
    byteArray: ByteArray,
    contentType: ContentType = ContentType.Application.OctetStream,
    cacheControl: String? = null,
    etag: String? = null,
): BinaryResponse = BinaryResponse(byteArray, contentType, cacheControl, etag)
