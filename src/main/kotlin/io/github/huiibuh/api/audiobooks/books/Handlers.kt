package io.github.huiibuh.api.audiobooks.books

import api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.scanner.saveToFile
import io.github.huiibuh.scanner.toTrackModel
import io.github.huiibuh.services.GetOrCreate
import io.github.huiibuh.utils.uriToFile
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

internal suspend fun OpenAPIPipelineResponseContext<BookModel>.patchBook(id: BookId, patchBook: PatchBook) {
    val book = transaction {
        val book = Book.findById(id.uuid) ?: throw APINotFound("Book could not be found")

        val tracks = Track.find { TTracks.book eq id.uuid }.toList()
        val trackReferences = tracks.toTrackModel()

        if (patchBook.title != null) {
            book.title = patchBook.title
            trackReferences.forEach { it.title = patchBook.title }
        }

        if (patchBook.language != null) {
            book.language = patchBook.language
            trackReferences.forEach { it.language = patchBook.language }
        }

        if (patchBook.description != null) {
            book.description = patchBook.description
            trackReferences.forEach { it.description = patchBook.description }
        }

        if (patchBook.asin != null) {
            book.asin = patchBook.asin
            trackReferences.forEach { it.asin = patchBook.asin }
        }

        if (patchBook.author != null) {
            val author = GetOrCreate.author(patchBook.author)
            book.author = author
            trackReferences.forEach { it.author = patchBook.author }
        }

        if (patchBook.narrator != null) {
            book.narrator = patchBook.narrator
            trackReferences.forEach { it.narrator = patchBook.narrator }
        }

        if (patchBook.series != null) {
            val series = GetOrCreate.series(patchBook.series, book.author)
            book.series = series
            trackReferences.forEach { it.series = patchBook.series }
        }

        if (patchBook.seriesIndex != null) {
            book.seriesIndex = patchBook.seriesIndex
            trackReferences.forEach { it.seriesIndex = patchBook.seriesIndex }
        }

        if (patchBook.cover != null) {
            val cover = patchBook.cover.uriToFile()
            book.cover = Image.new { image = ExposedBlob(cover) }
            trackReferences.forEach { it.cover = cover }
        }

        trackReferences.saveToFile()
        book
    }
    respond(book.toModel())
}
