package io.github.huiibuh.api.audiobooks.authors

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.github.huiibuh.api.exceptions.APINotImplemented
import io.github.huiibuh.models.AuthorModel


internal suspend fun OpenAPIPipelineResponseContext<AuthorModel>.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor) {
    throw APINotImplemented("Authors cannot get patched currently")
}
