package io.thoth.server.api.audiobooks.books

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.thoth.common.exceptions.APINotImplemented
import io.thoth.models.BookModel

internal suspend fun OpenAPIPipelineResponseContext<BookModel>.patchBook(id: BookId, patchBook: PatchBook) {
    throw APINotImplemented("Book modification is not yet supported")
}
