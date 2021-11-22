package io.github.huiibuh.api.audiobooks.authors

import api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.TTracks
import io.github.huiibuh.db.tables.Track
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.scanner.TrackReference
import io.github.huiibuh.scanner.saveToFile
import io.github.huiibuh.scanner.toTrackModel
import io.github.huiibuh.utils.uriToFile
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction


internal suspend fun OpenAPIPipelineResponseContext<AuthorModel>.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor) {
    val author = transaction {
        val author = Author.findById(id.uuid) ?: throw APINotFound("Author could not be found")

        val tracks = Track.find { TTracks.author eq id.uuid }.toList()
        val trackReferences = tracks.toTrackModel()

        if (patchAuthor.name != null) {
            trackReferences.forEach { it.author = patchAuthor.name }
            author.name = patchAuthor.name
        }

        if (patchAuthor.asin != null) {
            author.asin = patchAuthor.asin
        }

        if (patchAuthor.biography != null) {
            author.biography = patchAuthor.biography
        }

        if (patchAuthor.image != null) {
            author.image = Image.new {
                image = ExposedBlob(patchAuthor.image.uriToFile())
            }
        }
        trackReferences.saveToFile()
        author
    }
    respond(author.toModel())
}
