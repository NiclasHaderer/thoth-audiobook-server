package io.thoth.server.repositories

import io.thoth.metadata.MetadataAgent
import io.thoth.metadata.MetadataAgents
import io.thoth.metadata.responses.MetadataLanguage
import io.thoth.metadata.responses.MetadataRegion
import io.thoth.models.Author
import io.thoth.models.AuthorDetailed
import io.thoth.models.AuthorUpdate
import io.thoth.openapi.ktor.errors.ErrorResponse
import io.thoth.server.common.ImageDownloader
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.MatchableEntity
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.common.extensions.escape
import io.thoth.server.common.extensions.ilike
import io.thoth.server.database.access.getOrCreateImage
import io.thoth.server.database.tables.AuthorAgentMetadataTable
import io.thoth.server.database.tables.AuthorFileMetadataTable
import io.thoth.server.database.tables.AuthorMetadata
import io.thoth.server.database.tables.AuthorMetadataRow
import io.thoth.server.database.views.AuthorRow
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.AuthorUserMetadataTable
import io.thoth.server.database.tables.BookUserMetadataTable
import io.thoth.server.database.tables.MetadataLayer
import io.thoth.server.database.tables.bookIdsLinkedToAuthor
import io.thoth.server.database.tables.replaceBookAuthors
import io.thoth.server.database.views.bookAuthors
import io.thoth.server.database.views.booksToModels
import io.thoth.server.database.tables.create
import io.thoth.server.database.tables.layer
import io.thoth.server.database.views.seriesToModels
import io.thoth.server.database.views.toAuthorRow
import io.thoth.server.database.views.toBookRow
import io.thoth.server.database.views.toSeriesRow
import io.thoth.server.database.tables.write
import io.thoth.server.database.views.AuthorMetadataView
import io.thoth.server.database.views.BookAuthorView
import io.thoth.server.database.views.BookSeriesView
import io.thoth.server.database.views.BookMetadataView
import io.thoth.server.database.views.SeriesMetadataView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import java.time.Instant

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
            AuthorMetadataView
                .selectAll()
                .where { AuthorMetadataView.id eq id and (AuthorMetadataView.library eq libraryId) }
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
            AuthorMetadataView
                .selectAll()
                .where { matchesName(query) and (AuthorMetadataView.library eq libraryId) }
                .orderBy(AuthorMetadataView.name.lowerCase() to SortOrder.ASC)
                .limit(searchLimit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun search(
        userId: UUID,
        query: String,
    ): List<Author> =
        transaction {
            AuthorMetadataView
                .selectAll()
                .where { matchesName(query) }
                .orderBy(AuthorMetadataView.name.lowerCase() to SortOrder.ASC)
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
            val id = AuthorTable.create(libraryRepository.raw(libraryId).id)
            AuthorFileMetadataTable.write(AuthorMetadataRow(author = id, name = authorName))
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.AUTHOR, id, libraryId))
            raw(id, libraryId)
        }

    override fun createManual(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            val id =
                AuthorTable.create(
                    libraryRepository.raw(libraryId).id,
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
                metadataAgent.getAuthorByName(authorName, region, language).firstOrNull()
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
        val metadataAgent: MetadataAgent,
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
    ): List<Author> =
        transaction {
            AuthorMetadataView
                .selectAll()
                .where { AuthorMetadataView.library eq libraryId }
                .orderBy(AuthorMetadataView.name.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it.toAuthorRow().toModel() }
        }

    override fun get(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): AuthorDetailed =
        transaction {
            val author = raw(id, libraryId)

            val books =
                BookMetadataView
                    .join(BookAuthorView, JoinType.INNER, BookMetadataView.id, BookAuthorView.book)
                    .selectAll()
                    .where { BookAuthorView.author eq id }
                    .orderBy(BookMetadataView.title.lowerCase() to SortOrder.ASC)
                    .map { it.toBookRow() }
            val seriesIds =
                BookAuthorView
                    .join(BookSeriesView, JoinType.INNER, BookAuthorView.book, BookSeriesView.book)
                    .select(BookSeriesView.series)
                    .where { BookAuthorView.author eq id }
                    .mapTo(mutableSetOf()) { it[BookSeriesView.series] }
            val series =
                SeriesMetadataView
                    .selectAll()
                    .where { SeriesMetadataView.id inList seriesIds }
                    .orderBy(SeriesMetadataView.title.lowerCase() to SortOrder.ASC)
                    .map { it.toSeriesRow() }

            AuthorDetailed.fromModel(
                author = author.toModel(),
                books = booksToModels(books, userId),
                series = seriesToModels(series),
            )
        }

    override fun sorting(
        libraryId: UUID,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): List<UUID> =
        transaction {
            AuthorMetadataView
                .selectAll()
                .where { AuthorMetadataView.library eq libraryId }
                .orderBy(AuthorMetadataView.name.lowerCase() to order)
                .offset(offset)
                .limit(limit)
                .map { it[AuthorMetadataView.id] }
        }

    override fun position(
        id: UUID,
        libraryId: UUID,
        order: SortOrder,
    ): Long =
        transaction {
            val name = raw(id, libraryId).name.lowercase()
            AuthorMetadataView
                .selectAll()
                .where {
                    val precedes =
                        if (order == SortOrder.ASC) {
                            AuthorMetadataView.name.lowerCase() less name
                        } else {
                            AuthorMetadataView.name.lowerCase() greater name
                        }
                    precedes and (AuthorMetadataView.library eq libraryId)
                }.count()
        }

    override fun modify(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
        partial: AuthorUpdate,
    ): Author {
        val currentImage = raw(id, libraryId).imageID
        val newImage = imageDownloader.download(partial.image?.takeUnless { it == currentImage?.toString() })
        return transaction {
            val user = AuthorUserMetadataTable.layer(id)
            AuthorUserMetadataTable.write(
                user.copy(
                    name = partial.name ?: user.name,
                    provider = partial.provider ?: user.provider,
                    providerID = partial.providerID ?: user.providerID,
                    biography = partial.biography ?: user.biography,
                    website = partial.website ?: user.website,
                    bornIn = partial.bornIn ?: user.bornIn,
                    birthDate = partial.birthDate ?: user.birthDate,
                    deathDate = partial.deathDate ?: user.deathDate,
                    imageID = getOrCreateImage(newImage, currentImageID = user.imageID),
                ),
            )

            if (partial.books != null) {
                setBooks(id, partial.books.map { bookRepository.raw(it, libraryId).id }.toSet())
            }

            AuthorTable.update({ AuthorTable.id eq id }) {
                it[deferDeletionUntil] = Instant.now().plus(DEFER_DELETION_GRACE)
            }
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
            BookUserMetadataTable.write(BookUserMetadataTable.layer(bookId).copy(authorsSet = true))
        }
    }

    override fun total(libraryId: UUID): Long =
        transaction { AuthorTable.selectAll().where { AuthorTable.library eq libraryId }.count() }
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

private fun matchesName(query: String): Op<Boolean> =
    AuthorMetadataView.name ilike "%${escape(query)}%"
