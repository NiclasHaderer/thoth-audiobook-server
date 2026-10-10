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
import io.thoth.server.database.rows.booksToModels
import io.thoth.server.database.rows.lockBookField
import io.thoth.server.database.rows.seriesToModels
import io.thoth.server.database.rows.toAuthorRow
import io.thoth.server.database.rows.toBookRow
import io.thoth.server.database.rows.toSeriesRow
import io.thoth.server.database.rows.update
import io.thoth.server.database.tables.AuthorBookTable
import io.thoth.server.database.tables.AuthorField
import io.thoth.server.database.tables.AuthorTable
import io.thoth.server.database.tables.BookField
import io.thoth.server.database.tables.BookTable
import io.thoth.server.database.tables.SeriesBookTable
import io.thoth.server.database.tables.SeriesTable
import io.thoth.server.database.tables.authorLinksWithBooks
import io.thoth.server.database.tables.bookIdsLinkedToAuthor
import io.thoth.server.database.tables.create
import io.thoth.server.schedules.AutoMatchRequest
import io.thoth.server.schedules.AutoMatcher
import io.thoth.server.schedules.MatchableEntity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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
            val id = AuthorTable.create(libraryRepository.raw(libraryId).id, authorName, taggedName = authorName)
            autoMatcher.matchOnCommit(AutoMatchRequest(MatchableEntity.AUTHOR, id, libraryId))
            raw(id, libraryId)
        }

    override fun createManual(
        authorName: String,
        libraryId: UUID,
    ): AuthorRow =
        transaction {
            // Born an orphan: hidden and on the clock until the caller attaches books. The user named it, so a
            // later match must not rename it.
            val id =
                AuthorTable.create(
                    libraryRepository.raw(libraryId).id,
                    authorName,
                    deferDeletionUntil = Instant.now().plus(DEFER_DELETION_GRACE),
                )
            AuthorTable.update({ AuthorTable.id eq id }) { it[locked] = setOf(AuthorField.NAME) }
            raw(id, libraryId)
        }

    override fun autoMatch(
        userId: UUID,
        id: UUID,
        libraryId: UUID,
    ): Author {
        val (metadataAgent, authorName, region, language, preferFile) =
            transaction {
                val library = libraryRepository.raw(libraryId)
                AutoMatchQuery(
                    metadataAgents.forLibrary(library),
                    raw(id, libraryId).name,
                    library.region,
                    library.language,
                    library.preferEmbeddedMetadata,
                )
            }
        val result =
            runBlocking {
                metadataAgent.bestAuthorMatch(authorName, region, language)
            } ?: throw noMatch(authorName)
        val newImage = imageDownloader.download(result.imageURL)

        return transaction {
            val author = raw(id, libraryId)
            val write = AutomaticWrite(author.locked, onlyFillEmpty = preferFile)
            AuthorTable.update(
                author.copy(
                    name = write.value(AuthorField.NAME, author.name) { result.name },
                    provider = write.overwrite(AuthorField.PROVIDER, author.provider) { result.id.provider },
                    providerID = write.overwrite(AuthorField.PROVIDER_ID, author.providerID) { result.id.itemID },
                    biography = write.value(AuthorField.BIOGRAPHY, author.biography) { result.biography },
                    website = write.value(AuthorField.WEBSITE, author.website) { result.website },
                    bornIn = write.value(AuthorField.BORN_IN, author.bornIn) { result.bornIn },
                    birthDate = write.value(AuthorField.BIRTH_DATE, author.birthDate) { result.birthDate },
                    deathDate = write.value(AuthorField.DEATH_DATE, author.deathDate) { result.deathDate },
                    imageID = write.value(AuthorField.IMAGE_ID, author.imageID) { getOrCreateImage(newImage, it) },
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
        val preferFile: Boolean,
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
                authorLinksWithBooks
                    .selectAll()
                    .where { (AuthorBookTable.author eq id) and BookTable.visible }
                    .orderBy(BookTable.title to SortOrder.ASC)
                    .map { it.toBookRow() }
            val seriesIds =
                AuthorBookTable
                    .join(SeriesBookTable, JoinType.INNER, AuthorBookTable.book, SeriesBookTable.book)
                    .select(SeriesBookTable.series)
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
                locked = author.locked.sorted(),
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
            val author = raw(id, libraryId)
            val edit = UserEdit(author.locked, partial.unlock)
            val image = partial.image.map { it?.let { getOrCreateImage(newImage, currentImageID = currentImage) } }
            AuthorTable.update(
                author.copy(
                    name = edit.value(AuthorField.NAME, author.name, partial.name),
                    provider = edit.value(AuthorField.PROVIDER, author.provider, partial.provider),
                    providerID = edit.value(AuthorField.PROVIDER_ID, author.providerID, partial.providerID),
                    biography = edit.value(AuthorField.BIOGRAPHY, author.biography, partial.biography),
                    website = edit.value(AuthorField.WEBSITE, author.website, partial.website),
                    bornIn = edit.value(AuthorField.BORN_IN, author.bornIn, partial.bornIn),
                    birthDate = edit.value(AuthorField.BIRTH_DATE, author.birthDate, partial.birthDate),
                    deathDate = edit.value(AuthorField.DEATH_DATE, author.deathDate, partial.deathDate),
                    imageID = edit.value(AuthorField.IMAGE_ID, author.imageID, image),
                    locked = edit.locked,
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
        val current = bookIdsLinkedToAuthor(authorId).toSet()
        val removed = current - wanted
        val added = wanted - current
        AuthorBookTable.deleteWhere { (author eq authorId) and (book inList removed) }
        val stillAuthored =
            AuthorBookTable
                .select(AuthorBookTable.book)
                .where { AuthorBookTable.book inList removed }
                .mapTo(mutableSetOf()) { it[AuthorBookTable.book].value }
        if (!stillAuthored.containsAll(removed)) throw ErrorResponse.userError("A book must have at least one author")
        added.forEach { bookId ->
            AuthorBookTable.insert {
                it[book] = bookId
                it[author] = authorId
            }
        }
        (removed + added).forEach { lockBookField(it, BookField.AUTHORS) }
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
    AuthorTable
        .select(AuthorTable.id)
        .where {
            ((AuthorTable.name ilike pattern) or (AuthorTable.taggedName ilike pattern)) and
                (AuthorTable.library eq libraryId)
        }.firstOrNull()
        ?.get(AuthorTable.id)
        ?.value

private fun matchesName(query: String): Op<Boolean> = AuthorTable.name ilike "%${escape(query)}%"
