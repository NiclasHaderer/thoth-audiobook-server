package io.thoth.server.api.images

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import java.io.InputStream
import java.util.*


@Path("{id}")
internal class ImageId(
    @PathParam("The id of the image you want to get") val id: UUID,
)


private const val png = "image/png"
private const val jpgJPEG = "image/jpeg"
private const val webp = "image/webp"

@BinaryResponse([png, jpgJPEG, webp])
internal class RawImageFile(val image: InputStream)
