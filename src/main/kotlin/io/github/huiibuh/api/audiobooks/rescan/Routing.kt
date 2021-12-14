package io.github.huiibuh.api.audiobooks.rescan

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.services.Scanner
import io.ktor.application.*
import io.ktor.response.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun NormalOpenAPIRoute.registerRescan(path: String = "rescan") {
    route(path) {
        tag(ApiTags.Rescan) {
            routing()
        }
    }
}

@OptIn(DelicateCoroutinesApi::class)
internal fun NormalOpenAPIRoute.routing() {
    post<Unit, Unit, Unit> { _, _ ->
        pipeline.call.respond(Unit)
        GlobalScope.launch {
            Scanner.rescan()
        }
    }
}
