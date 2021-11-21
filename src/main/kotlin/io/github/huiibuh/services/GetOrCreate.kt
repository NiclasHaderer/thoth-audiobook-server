package io.github.huiibuh.services

import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.tables.*
import io.github.huiibuh.db.tables.Series
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

object GetOrCreate {
    fun author(name: String) = transaction {
        val author = Author.findOne { TAuthors.name like name }
        author ?: Author.new {
            this.name = name
        }
    }

    fun book(
        name: String,
        author: Author,
        composer: Author?,
        series: Series?,
        seriesIndex: Int?,
        cover: ByteArray?,
    ) = transaction {
        val book = Book.findOne { TBooks.title like name and (TBooks.author eq author.id.value) }
        book
            ?: Book.new {
                this.title = name
                this.author = author
                this.narrator = composer
                this.series = series
                this.seriesIndex = seriesIndex
                this.cover = if (cover != null) image(cover) else null
            }
    }

    fun series(name: String, author: Author) = transaction {
        val series = Series.findOne { TSeries.title like name }
        series ?: Series.new {
            this.title = name
            this.author = author
        }
    }

    fun image(imageBytes: ByteArray) = transaction {
        val imageBlob = ExposedBlob(imageBytes)

        val img = Image.findOne { TImages.image eq imageBlob }
        img ?: Image.new { image = imageBlob }
    }
}
