package io.thoth.server.api.images

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.thoth.common.exceptions.withNotFoundHandling
import io.thoth.database.tables.Image
import io.thoth.server.api.ApiTags
import io.thoth.server.api.audiobooks.QueryLimiter
import java.io.ByteArrayInputStream
import java.util.*


fun NormalOpenAPIRoute.registerImageRouting(route: String = "image") {
    tag(ApiTags.Files) {
        withNotFoundHandling {
            route(route) {
                imageRouting()
            }
        }
    }
}

internal fun NormalOpenAPIRoute.imageRouting() {
    get<ImageId, RawImageFile>(
        info("View image")
    ) {
        val image = Image.getById(it.id)
        respond(
            RawImageFile(ByteArrayInputStream(image.image))
        )
    }
    get<QueryLimiter, List<UUID>>(
        info("List images")
    ) {
        val images = Image.getMultiple(it.limit, it.offset)
        respond(images)
    }
}



