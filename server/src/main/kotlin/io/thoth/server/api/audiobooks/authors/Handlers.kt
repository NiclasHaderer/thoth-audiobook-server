package io.thoth.server.api.audiobooks.authors

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.thoth.common.exceptions.APINotImplemented
import io.thoth.models.AuthorModel


internal suspend fun OpenAPIPipelineResponseContext<AuthorModel>.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor) {
    throw APINotImplemented("Author modification is not yet supported")
}
