package io.github.huiibuh.api.images

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import io.github.huiibuh.api.stream.*
import java.io.InputStream
import java.util.*


@Path("{id}")
data class ImageId(
    @PathParam("The id of the image you want to get") val id: UUID,
)


const val png = "image/png"
const val jpgJPEG = "image/jpeg"
const val webp = "image/webp"

@BinaryResponse([png, jpgJPEG, webp])
data class RawImageFile(val image: InputStream)
