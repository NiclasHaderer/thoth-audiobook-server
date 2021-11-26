package io.github.huiibuh.services

import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TImages
import io.github.huiibuh.db.tables.TSeries
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction

object GetOrCreate {
    fun author(
        name: String,
        biography: String? = null,
        asin: String? = null,
        image: Image? = null,
    ) = transaction {
        val author = Author.findOne { TAuthors.name like name }
        author ?: Author.new {
            this.name = name
            this.biography = biography
            this.asin = asin
            this.image = image
        }
    }

    fun book(
        title: String,
        year: Int?,
        language: String?,
        description: String?,
        asin: String?,
        author: Author,
        narrator: Author?,
        series: Series?,
        seriesIndex: Int?,
        cover: ByteArray?,
    ) = transaction {
        val book = Book.findOne { TBooks.title like title and (TBooks.author eq author.id.value) }
        book
            ?: Book.new {
                this.title = title
                this.year = year
                this.language = language
                this.description = description
                this.asin = asin
                this.author = author
                this.narrator = narrator
                this.series = series
                this.seriesIndex = seriesIndex
                this.cover = if (cover != null) image(cover) else null
            }
    }

    fun series(
        title: String,
        author: Author,
        asin: String? = null,
        description: String? = null,
    ) = transaction {
        val series = Series.findOne { TSeries.title like title }
        series ?: Series.new {
            this.title = title
            this.asin = asin
            this.description = description
            this.author = author
        }
    }

    fun image(imageBytes: ByteArray) = transaction {
        val imageBlob = ExposedBlob(imageBytes)

        val img = Image.findOne { TImages.image eq imageBlob }
        img ?: Image.new { image = imageBlob }
    }
}
