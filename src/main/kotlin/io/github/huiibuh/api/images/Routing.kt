package io.github.huiibuh.api.images

import api.exceptions.withNotFoundHandling
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.services.database.ImageService
import java.io.ByteArrayInputStream


fun NormalOpenAPIRoute.registerImageRouting(route: String = "image") {
    tag(ApiTags.Files) {
        withNotFoundHandling {
            route(route) {
                imageRouting()
            }
        }
    }
}

fun NormalOpenAPIRoute.imageRouting() {

    get<ImageId, RawImageFile> {
        val image = ImageService.get(it.id)
        respond(
            RawImageFile(ByteArrayInputStream(image.image))
        )
    }
}



