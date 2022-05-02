package io.thoth.server.api.audiobooks.rescan

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.application.*
import io.ktor.response.*
import io.thoth.server.api.ApiTags
import io.thoth.server.file.scanner.CompleteScan

fun NormalOpenAPIRoute.registerRescan(path: String = "rescan") {
    route(path) {
        tag(ApiTags.Rescan) {
            routing()
        }
    }
}

internal fun NormalOpenAPIRoute.routing() {
    post<Unit, Unit, Unit> { _, _ ->
        pipeline.call.respond(Unit)
        CompleteScan().start()
    }
}
