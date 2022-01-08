package io.github.huiibuh.api.stream

import io.github.huiibuh.api.exceptions.APINotFound
import io.github.huiibuh.api.exceptions.withNotFoundHandling
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.services.database.TrackService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import java.io.File


fun NormalOpenAPIRoute.registerStreamingRouting(route: String = "audio") {
    tag(ApiTags.Files) {
        withNotFoundHandling {
            route(route) {
                streamingRouting()
            }
        }
    }
}

fun NormalOpenAPIRoute.streamingRouting() {
    get<AudioId, RawAudioFile>(
        info("Stream a audio file")
    ) { fileId ->
        val track = TrackService.get(fileId.id)
        val file = File(track.path)
        if (!file.exists()) {
            throw APINotFound("Database out of sync. Please start syncing process.")
        }
        pipeline.call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, file.name).toString()
        )
        pipeline.call.respondFile(file)
    }
}



