package io.github.huiibuh.api.audiobooks.authors

import io.github.huiibuh.api.exceptions.APIBadRequest
import io.github.huiibuh.api.exceptions.APINotFound
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.extensions.uriToFile
import io.github.huiibuh.file.tagger.saveToFile
import io.github.huiibuh.file.tagger.toTrackModel
import io.github.huiibuh.models.AuthorModel
import io.github.huiibuh.services.RemoveEmpty
import io.github.huiibuh.services.database.ImageService
import io.github.huiibuh.services.database.TrackService
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*


internal suspend fun OpenAPIPipelineResponseContext<AuthorModel>.patchAuthor(id: AuthorId, patchAuthor: PatchAuthor) {
    val author = transaction {
        val author = Author.findById(id.uuid) ?: throw APINotFound("Author could not be found")

        // Attributes cannot be empty
        if (patchAuthor.name.isEmpty()) {
            throw APIBadRequest("Author name cannot be enpty")
        }

        val tracks = TrackService.forAuthor(id.uuid)
        val trackReferences = tracks.toTrackModel()

        if (patchAuthor.name != author.name) {
            trackReferences.forEach { it.author = patchAuthor.name }
            author.name = patchAuthor.name
        }

        val newProviderID = patchAuthor.providerID
        if (ProviderID.eq(author.providerID, newProviderID)) {
            author.providerID = ProviderID.newFrom(newProviderID)
        }

        if (patchAuthor.biography != author.biography) {
            author.biography = patchAuthor.biography
        }

        if (patchAuthor.image != null) {
            author.image = Image.new {
                image = ExposedBlob(patchAuthor.image.uriToFile())
            }
        }

        val patchImage = try {
            ImageService.get(UUID.fromString(patchAuthor.image))
        } catch (_: Exception) {
            null
        }

        if ( // Cover exists or is null for the new and the old values
            patchImage?.id != author.image?.id?.value ||
            // Cover does not exist and the base64 representation is not the same
            patchImage == null && patchAuthor.image != Base64.getEncoder().encode(author.image?.image?.bytes).toString()
        ) {
            val cover = patchAuthor.image?.uriToFile()
            author.image = if (cover == null) null else Image.new { image = ExposedBlob(cover) }
        }

        trackReferences.saveToFile()
        flushCache()
        author
    }
    respond(author.toModel())
    RemoveEmpty.all()
}
