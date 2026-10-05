package io.thoth.server.database.tables

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.date

object AuthorTable : LibraryEntityTable("author", AuthorField.NAME.column) {
    val biography = text(AuthorField.BIOGRAPHY.column).nullable()
    val website = varchar(AuthorField.WEBSITE.column, 255).nullable()
    val birthDate = date(AuthorField.BIRTH_DATE.column).nullable()
    val bornIn = varchar(AuthorField.BORN_IN.column, 255).nullable()
    val deathDate = date(AuthorField.DEATH_DATE.column).nullable()
    val provider = varchar(AuthorField.PROVIDER.column, 255).nullable()
    val providerId = varchar(AuthorField.PROVIDER_ID.column, 255).nullable()
    val imageId = reference(AuthorField.IMAGE_ID.column, ImageTable, onDelete = ReferenceOption.SET_NULL).nullable()
}
