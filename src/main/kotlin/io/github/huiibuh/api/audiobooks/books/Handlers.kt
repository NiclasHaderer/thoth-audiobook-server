package io.github.huiibuh.api.audiobooks.books

import api.exceptions.APIBadRequest
import api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.extensions.uriToFile
import io.github.huiibuh.models.BookModel
import io.github.huiibuh.scanner.saveToFile
import io.github.huiibuh.scanner.toTrackModel
import io.github.huiibuh.services.GetOrCreate
import io.github.huiibuh.services.RemoveEmpty
import io.github.huiibuh.services.database.ImageService
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

internal suspend fun OpenAPIPipelineResponseContext<BookModel>.patchBook(id: BookId, patchBook: PatchBook) {
    val book = transaction {
        val book = Book.findById(id.uuid) ?: throw APINotFound("Book could not be found")

        val tracks = Track.find { TTracks.book eq id.uuid }.toList()
        val trackReferences = tracks.toTrackModel()

        // Attributes cannot be empty
        if (patchBook.title.isEmpty() || patchBook.author.isEmpty()) {
            throw APIBadRequest("Title and Author cannot be empty strings")
        }

        // Update title
        if (book.title != patchBook.title) {
            book.title = patchBook.title
            trackReferences.forEach { it.title = patchBook.title }
        }

        // Update author
        if (patchBook.author != book.author.name) {
            val author = GetOrCreate.author(patchBook.author)
            book.author = author
            trackReferences.forEach { it.author = patchBook.author }
        }

        if (patchBook.language != book.language) {
            book.language = patchBook.language
            trackReferences.forEach { it.language = patchBook.language }
        }

        if (patchBook.description != book.description) {
            book.description = patchBook.description
            trackReferences.forEach { it.description = patchBook.description }
        }

        val newProviderID = patchBook.providerID
        if (ProviderID.eq(book.providerID, newProviderID)) {
            book.providerID = ProviderID.newFrom(newProviderID)
        }

        if (patchBook.narrator != book.narrator) {
            book.narrator = patchBook.narrator
            trackReferences.forEach { it.narrator = patchBook.narrator }
        }

        if (patchBook.series != book.series?.title) {
            val series = if (patchBook.series != null) GetOrCreate.series(patchBook.series, book.author) else null
            book.series = series
            trackReferences.forEach { it.series = patchBook.series }
        }

        if (patchBook.seriesIndex != book.seriesIndex) {
            book.seriesIndex = patchBook.seriesIndex
            trackReferences.forEach { it.seriesIndex = patchBook.seriesIndex }
        }
        if (patchBook.year != book.year) {
            book.year = patchBook.year
            trackReferences.forEach { it.year = patchBook.year }
        }

        val patchCover = try {
            ImageService.get(UUID.fromString(patchBook.cover))
        } catch (_: Exception) {
            null
        }

        if ( // Cover exists or is null for the new and the old values
            patchCover?.id != book.cover?.id?.value ||
            // Cover does not exist and the base64 representation is not the same
            patchCover == null && patchBook.cover != Base64.getEncoder().encode(book.cover?.image?.bytes).toString()
        ) {
            val cover = patchBook.cover?.uriToFile()
            book.cover = if (cover == null) null else Image.new { image = ExposedBlob(cover) }
            trackReferences.forEach { it.cover = cover }
        }

        trackReferences.saveToFile()
        flushCache()
        book.toModel()
    }
    respond(book)
    RemoveEmpty.all()
}
