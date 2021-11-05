package io.github.huiibuh.api.audiobooks

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.put
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.ktor.application.*


fun Application.registerAudiobookRouting() {
    apiRouting {
        tag(ApiTags.Audiobook) {
            route("audiobooks") {
                audiobookRouting()
            }
        }
    }
}

fun NormalOpenAPIRoute.audiobookRouting() {
    get<AudiobookId, Audiobook>(
        info("Get one Audiobooks")
    ) { params ->
        respond(Audiobook(""))
    }

    get<Unit, Array<Audiobook>>(
        info("Get all Audiobook")
    ) { params ->
        respond(arrayOf())
    }
    put<AudiobookId, Audiobook, Audiobook>(
        info("Update one audiobook")
    ) { id, audiobook ->
        respond(Audiobook(""))
    }
}

