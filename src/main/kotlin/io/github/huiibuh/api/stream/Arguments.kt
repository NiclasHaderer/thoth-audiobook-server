package io.github.huiibuh.api.stream

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import java.io.InputStream
import java.util.*


@Path("{id}")
class AudioId(
    @PathParam("The id of the file you want to stream") val id: UUID,
)

const val mp3 = "audio/mpeg"
const val flac = "audio/flac"
const val ogg = "audio/ogg"
const val vobis = "audio/vobis"
const val m4a = "audio/m4a"
const val m4p = "audio/m4p"
const val m4b = "audio/m4b"
const val aiff = "audio/aiff"
const val wav = "audio/wav"
const val wma = "audio/wma"
const val dsf = "audio/dsf"

@BinaryResponse([mp3, flac, ogg, vobis, m4a, m4p, m4b, aiff, wav, wma, dsf])
class RawAudioFile(val stream: InputStream)
