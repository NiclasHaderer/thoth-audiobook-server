package io.thoth.server.database.views

import io.thoth.server.common.exposed.View
import io.thoth.server.database.tables.AuthorBookTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.jdbc.select

object BookAuthorView : View("BookAuthor") {
    val book = javaUUID("book")
    val author = javaUUID("author")

    override fun body() =
        AuthorBookTable
            .join(BookMetadataView, JoinType.INNER, AuthorBookTable.book, BookMetadataView.id)
            .select(AuthorBookTable.book, AuthorBookTable.authors)
            .where { AuthorBookTable.addedBy eq BookMetadataView.authorsFrom }
}
