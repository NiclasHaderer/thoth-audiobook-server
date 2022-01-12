package io.github.huiibuh.api.images

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.github.huiibuh.api.ApiTags
import io.github.huiibuh.api.exceptions.withNotFoundHandling
import io.github.huiibuh.db.tables.Image
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

internal fun NormalOpenAPIRoute.imageRouting() {
    get<ImageId, RawImageFile> {
        val image = Image.getById(it.id)
        respond(
            RawImageFile(ByteArrayInputStream(image.image))
        )
    }
}



