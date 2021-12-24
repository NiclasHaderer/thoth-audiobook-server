package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

@Path("{uuid}")
internal data class AuthorId(
    @PathParam("The id of the author you want to get") val uuid: UUID,
)

internal data class PatchAuthor(
    val name: String,
    val biography: String?,
    val asin: String?,
    val image: String?,
)
