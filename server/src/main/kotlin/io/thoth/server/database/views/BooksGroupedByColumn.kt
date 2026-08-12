package io.thoth.server.database.views

import io.thoth.server.common.extensions.jsonEach
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Count
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

data class BookGroup(
    val name: String,
    val bookCount: Int,
)

data class BooksInGroup(
    val name: String,
    val books: List<BookRow>,
)

context(_: Transaction)
fun Column<List<String>?>.bookGroups(
    libraryId: UUID,
    order: SortOrder,
    limit: Int,
    offset: Long,
): List<BookGroup> {
    val eachName = jsonEach()
    val groupName = eachName.value.min()
    val bookCount = BookMetadataView.id.countDistinct()
    return BookMetadataView
        .crossJoin(eachName)
        .select(groupName, bookCount)
        .where { BookMetadataView.library eq libraryId }
        .groupBy(eachName.value.lowerCase())
        .orderBy(groupName.lowerCase() to order)
        .offset(offset)
        .limit(limit)
        .map { BookGroup(name = it[groupName]!!, bookCount = it[bookCount].toInt()) }
}

context(_: Transaction)
fun Column<List<String>?>.bookGroupCount(libraryId: UUID): Long {
    val eachName = jsonEach()
    val groups = Count(eachName.value.lowerCase(), distinct = true)
    return BookMetadataView
        .crossJoin(eachName)
        .select(groups)
        .where { BookMetadataView.library eq libraryId }
        .first()[groups]
}

context(_: Transaction)
fun Column<List<String>?>.booksInGroup(
    name: String,
    libraryId: UUID,
): BooksInGroup? {
    val eachName = jsonEach()
    val nameMatches =
        exists(
            eachName
                .select(eachName.value)
                .where { eachName.value.lowerCase() eq stringParam(name).lowerCase() },
        )
    val books =
        BookMetadataView
            .selectAll()
            .where { (BookMetadataView.library eq libraryId) and nameMatches }
            .orderBy(BookMetadataView.title.lowerCase() to SortOrder.ASC)
            .toList()
    if (books.isEmpty()) return null
    val groupName = books.flatMap { it[this].orEmpty() }.filter { it.equals(name, ignoreCase = true) }.min()
    return BooksInGroup(name = groupName, books = books.map { it.toBookRow() })
}
