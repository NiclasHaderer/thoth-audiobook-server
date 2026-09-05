package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

context(_: Transaction)
fun bookIdsLinkedToSeries(seriesId: UUID): List<UUID> =
    SeriesBookTable
        .select(SeriesBookTable.book)
        .where { SeriesBookTable.series eq seriesId }
        .mapTo(mutableSetOf()) { it[SeriesBookTable.book].value }
        .toList()

context(_: Transaction)
fun bookIdsLinkedToAuthor(authorId: UUID): List<UUID> =
    AuthorBookTable
        .select(AuthorBookTable.book)
        .where { AuthorBookTable.authors eq authorId }
        .mapTo(mutableSetOf()) { it[AuthorBookTable.book].value }
        .toList()

context(_: Transaction)
fun seriesIdsLinkedToBook(bookId: UUID): List<UUID> =
    SeriesBookTable
        .select(SeriesBookTable.series)
        .where { SeriesBookTable.book eq bookId }
        .mapTo(mutableSetOf()) { it[SeriesBookTable.series].value }
        .toList()

context(_: Transaction)
fun authorIdsLinkedToBook(bookId: UUID): List<UUID> =
    AuthorBookTable
        .select(AuthorBookTable.authors)
        .where { AuthorBookTable.book eq bookId }
        .mapTo(mutableSetOf()) { it[AuthorBookTable.authors].value }
        .toList()

context(_: Transaction)
fun replaceBookAuthors(
    bookId: UUID,
    source: MetadataLayer,
    authorIds: Collection<UUID>,
) = with(AuthorBookTable) {
    val mine = (book eq bookId) and (AuthorBookTable.addedBy eq source)
    val wanted = authorIds.toSet()
    val existing =
        select(authors).where { mine }.mapTo(mutableSetOf()) { it[authors].value }

    val stale = existing - wanted
    if (stale.isNotEmpty()) deleteWhere { mine and (authors inList stale) }
    (wanted - existing).forEach { authorId ->
        insert {
            it[book] = bookId
            it[authors] = authorId
            it[AuthorBookTable.addedBy] = source
        }
    }
}

context(_: Transaction)
fun replaceBookSeries(
    bookId: UUID,
    source: MetadataLayer,
    targets: Map<UUID, Float?>,
) = with(SeriesBookTable) {
    val mine = (book eq bookId) and (SeriesBookTable.addedBy eq source)
    deleteWhere { mine and (series notInList targets.keys) }
    targets.forEach { (seriesId, index) ->
        val updated = update({ mine and (series eq seriesId) }) { it[seriesIndex] = index }
        if (updated == 0) {
            insert {
                it[book] = bookId
                it[series] = seriesId
                it[seriesIndex] = index
                it[SeriesBookTable.addedBy] = source
            }
        }
    }
}
