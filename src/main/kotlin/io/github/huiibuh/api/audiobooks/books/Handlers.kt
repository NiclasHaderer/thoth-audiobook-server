package io.github.huiibuh.api.audiobooks.books

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.github.huiibuh.api.exceptions.APINotImplemented
import io.github.huiibuh.models.BookModel

internal suspend fun OpenAPIPipelineResponseContext<BookModel>.patchBook(id: BookId, patchBook: PatchBook) {
    throw APINotImplemented("Book modification is not yet supported")
}
