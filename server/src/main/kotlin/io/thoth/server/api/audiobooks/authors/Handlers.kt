package io.thoth.server.api.audiobooks.authors

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.thoth.models.datastructures.AuthorModel
import io.thoth.models.exceptions.APINotImplemented


internal suspend fun OpenAPIPipelineResponseContext<AuthorModel>.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor) {
    throw APINotImplemented("Author modification is not yet supported")
}
