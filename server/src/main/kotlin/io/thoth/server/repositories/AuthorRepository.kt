package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgentWrapper
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.Author
import io.thoth.models.AuthorDetailed
import io.thoth.models.AuthorUpdate
import io.thoth.openapi.common.ifSet
import io.thoth.openapi.common.map
import io.thoth.openapi.common.orElse
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.common.exposed.unless
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.rows.AuthorRow
import io.thoth.server.database.rows.bookAuthors
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.rows.seriesToModels
import io.thoth.server.database.rows.toAuthorRow
import io.thoth.server.database.rows.toBookRow
import io.thoth.server.database.rows.toSeriesRow
import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorField
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorMetadata
import io.thoth.server.database.tables.AuthorMetadataRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.bookIdsLinkedToAuthor
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.tables.resolvedAuthorLinks
import io.thoth.server.database.tables.write
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.schedules.MatchableEntity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.UUID

interface AuthorRepository : Repository<AuthorRow, Author, AuthorDetailed, AuthorUpdate> {
    fun findByTaggedName(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow?

    fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow

    fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow

    fun createManual(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow
}

class AuthorServiceImpl :
    AuthorRepository,
    KoinComponent {
    val metadataAgents by inject<MetadataAgents>()
    val libraryRepository by inject<LibraryRepository>()
    private val bookRepository by inject<BookRepository>()
    private val imageDownloader by inject<ImageDownloader>()
    private val autoMatcher by inject<AutoMatcher>()

    override fun findByTaggedName(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow? =
        transaction {
            idOfTaggedName(escape(authorName), libraryId)?.let { raw(it, libraryId) }
        }

    override fun raw(
        id: UUID,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            AuthorTable
                .selectAll()
                .where { AuthorTable.id eq id and (AuthorTable.library eq libraryId) }
                .firstOrNull()
                ?.toAuthorRow()
                ?: throw ErrorResponse.notFound("Author", id)
        }

    override fun search(
        userId: UUID,
        query: String,
        libraryId: UUID,
    ): List<Author> =
        transaction {
            AuthorTable
                .selectAll()
                .where {
                    matchesName(
                        query,
                    ) and (AuthorTable.library eq libraryId) and AuthorTable.visible
                }.orderBy(AuthorTable.name to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun getOrCreate(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow = transaction { findByTaggedName(authorName, libraryId) ?: create(authorName, libraryId) }

    override fun create(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            val id = AuthorTable.create(libraryRepository.raw(libraryId).id, authorName)
            AuthorFileMetadataTable.write(AuthorMetadataRow(author = id, name = authorName))
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.AUTHOR, id, libraryId))
            raw(id, libraryId)
        }

    override fun createManual(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            // Born an orphan: hidden and on the clock until the caller attaches books
            val id =
                AuthorTable.create(
                    libraryRepository.raw(libraryId).id,
                    authorName,
                    deferDeletionUntil = Instant.now().plus(DEFER_DELETION_GRACE),
                )
            AuthorUserMetadataTable.write(AuthorMetadataRow(author = id, name = authorName))
            raw(id, libraryId)
        }

    override fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): Author {
        val (metadataAgent, authorName, region, language) =
            transaction {
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    raw(id, libraryId).name,
                    library.region,
                    library.language,
                )
            }
        val result =
            runBlocking {
                metadataAgent.bestAuthorMatch(authorName, region, language)
            } ?: throw noMatch(authorName)
        val newImage = imageDownloader.download(result.imageURL)

        return transaction {
            val agent = AuthorAgentMetadataTable.layer(id)
            AuthorAgentMetadataTable.write(
                agent.copy(
                    name = result.name ?: agent.name,
                    provider = result.id.provider,
                    providerID = result.id.itemID,
                    biography = result.biography ?: agent.biography,
                    website = result.website ?: agent.website,
                    bornIn = result.bornIn ?: agent.bornIn,
                    birthDate = result.birthDate ?: agent.birthDate,
                    deathDate = result.deathDate ?: agent.deathDate,
                    imageID = getOrCreateImage(newImage, currentImageID = agent.imageID),
                ),
            )
            raw(id, libraryId).toModel()
        }
    }

    private data class AutoMatchQuery(
        val metadataAgent: MetadataAgentWrapper,
        val authorName: String,
        val region: MetadataRegion,
        val language: MetadataLanguage,
    )

    override fun getAll(
        userId: UUID,
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
        showInvisible: Boolean,
    ): List<Author> =
        transaction {
            AuthorTable
                .selectAll()
                .where {
                    (AuthorTable.library eq libraryId) and AuthorTable.visible.unless(showInvisible)
                }.orderBy(AuthorTable.name to order)
                .offset(offset)
                .limit(limit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        showInvisible: Boolean,
    ): AuthorDetailed =
        transaction {
            val author = raw(id, libraryId)
            if (!showInvisible) requireVisible(AuthorTable, "Author", id)

            val books =
                resolvedAuthorLinks
                    .selectAll()
                    .where { (AuthorBookTable.author eq id) and BookTable.visible }
                    .orderBy(BookTable.title to SortOrder.ASC)
                    .map { it.toBookRow() }
            val seriesIds =
                resolvedAuthorLinks
                    .join(SeriesBookTable, JoinType.INNER, AuthorBookTable.book, SeriesBookTable.book) {
                        SeriesBookTable.addedBy eq BookTable.seriesFrom
                    }.select(SeriesBookTable.series)
                    .where { AuthorBookTable.author eq id }
                    .mapTo(mutableSetOf()) { it[SeriesBookTable.series].value }
            val series =
                SeriesTable
                    .selectAll()
                    .where { (SeriesTable.id inList seriesIds) and SeriesTable.visible }
                    .orderBy(SeriesTable.title to SortOrder.ASC)
                    .map { it.toSeriesRow() }

            AuthorDetailed.fromModel(
                author = author.toModel(),
                books = booksToModels(books, userId),
                series = seriesToModels(series),
            )
        }

    override fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: AuthorUpdate,
    ): Author {
        val currentImage = raw(id, libraryId).imageID
        val newImage =
            imageDownloader.download(partial.image.orElse(null)?.takeUnless { it == currentImage?.toString() })
        return transaction {
            val user = AuthorUserMetadataTable.layer(id)
            val edit = LayerEdit(user.claimed)
            AuthorUserMetadataTable.write(
                user.copy(
                    name = edit.value(AuthorField.NAME, partial.name, user.name),
                    provider = edit.value(AuthorField.PROVIDER, partial.provider, user.provider),
                    providerID = edit.value(AuthorField.PROVIDER_ID, partial.providerID, user.providerID),
                    biography = edit.value(AuthorField.BIOGRAPHY, partial.biography, user.biography),
                    website = edit.value(AuthorField.WEBSITE, partial.website, user.website),
                    bornIn = edit.value(AuthorField.BORN_IN, partial.bornIn, user.bornIn),
                    birthDate = edit.value(AuthorField.BIRTH_DATE, partial.birthDate, user.birthDate),
                    deathDate = edit.value(AuthorField.DEATH_DATE, partial.deathDate, user.deathDate),
                    imageID =
                        edit.value(
                            AuthorField.IMAGE_ID,
                            partial.image.map { it?.let { getOrCreateImage(newImage, currentImageID = currentImage) } },
                            user.imageID,
                        ),
                    claimed = edit.claimed,
                ),
            )

            partial.books.ifSet { books ->
                setBooks(id, books.map { bookRepository.raw(it, libraryId).id }.toSet())
            }

            refreshAuthorDeferral(listOf(id))
            raw(id, libraryId).toModel()
        }
    }

    context(_: Transaction)
    private fun setBooks(
        authorId: UUID,
        wanted: Set<UUID>,
    ) {
        val affected = (bookIdsLinkedToAuthor(authorId) + wanted).distinct()
        val resolved = bookAuthors(affected)
        affected.forEach { bookId ->
            val current = resolved[bookId].orEmpty().map { it.id }.toSet()
            val next = if (bookId in wanted) current + authorId else current - authorId
            if (next == current) return@forEach
            if (next.isEmpty()) throw ErrorResponse.userError("A book must have at least one author")
            replaceBookAuthors(bookId, MetadataLayer.USER, next)
            val layer = BookUserMetadataTable.layer(bookId)
            BookUserMetadataTable.write(layer.copy(claimed = layer.claimed + BookField.AUTHORS))
        }
    }

    override fun total(
        libraryId: UUID,
        showInvisible: Boolean,
    ): Long =
        transaction {
            AuthorTable
                .selectAll()
                .where {
                    (AuthorTable.library eq libraryId) and AuthorTable.deferDeletionUntil.isNull().unless(showInvisible)
                }.count()
        }
}

context(_: Transaction)
private fun idOfTaggedName(
    pattern: String,
    libraryId: UUID,
): UUID? =
    idInLayer(AuthorFileMetadataTable, pattern, libraryId)
        ?: idInLayer(AuthorUserMetadataTable, pattern, libraryId)
        ?: idInLayer(AuthorAgentMetadataTable, pattern, libraryId)

context(_: Transaction)
private fun idInLayer(
    table: AuthorMetadata,
    pattern: String,
    libraryId: UUID,
): UUID? =
    (AuthorTable innerJoin table)
        .select(AuthorTable.id)
        .where { (table.name ilike pattern) and (AuthorTable.library eq libraryId) }
        .firstOrNull()
        ?.get(AuthorTable.id)
        ?.value

private fun matchesName(query: String): Op<Boolean> = AuthorTable.name ilike "%${escape(query)}%"
