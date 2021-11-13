package io.github.huiibuh.api.stream

import api.exceptions.APINotFound
import api.exceptions.withNotFoundHandling
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.db.models.Track
import io.github.huiibuh.services.database.TrackService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File


fun NormalOpenAPIRoute.registerStreamingRouting(route: String = "stream") {
    tag(ApiTags.Stream) {
        withNotFoundHandling {
            route(route) {
                streamingRouting()
            }
        }
    }
}

fun NormalOpenAPIRoute.streamingRouting() {
    get<FileId, RawAudioFile>(
        info("Stream a file")
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



