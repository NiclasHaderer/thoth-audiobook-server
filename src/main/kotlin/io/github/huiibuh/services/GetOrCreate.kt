package io.github.huiibuh.services

import api.exceptions.ApiException
import io.github.huiibuh.db.findOne
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TImages
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.utils.imageFromString
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object GetOrCreate {
    private val log = LoggerFactory.getLogger(this::class.java)


    fun author(
        name: String,
        biography: String? = null,
        asin: String? = null,
        image: Image? = null,
    ) = transaction {
        var author = Author.findOne { TAuthors.name like name }
        if (author != null) return@transaction author

        val authorInfo = try {
            runBlocking { AudibleService.getAuthorByName(name) }
        } catch (e: ApiException) {
            log.debug("$e, Author: $name")
            null
        }

        if (authorInfo == null) {
            log.debug("Could not find author information for $author")
        }

        author = Author.new {
            this.name = name
            this.biography = biography ?: authorInfo?.biography
            this.asin = asin ?: authorInfo?.asin
            this.image = image ?: if (authorInfo?.image != null) {
                GetOrCreate.image(runBlocking { imageFromString(authorInfo.image!!) })
            } else null
        }
        return@transaction author
    }

    fun book(
        title: String,
        year: Int?,
        language: String?,
        description: String?,
        asin: String?,
        author: Author,
        narrator: String?,
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
        if (series != null) return@transaction series

        val seriesInfo = try {
            runBlocking { AudibleService.getSeriesByName(title) }
        } catch (e: ApiException) {
            log.debug("$e, Series: $title")
            null
        }

        if (seriesInfo == null) {
            log.debug("Could not find series information for ${author.name}")
        }

        Series.new {
            this.title = title
            this.asin = asin ?: seriesInfo?.asin
            this.description = description ?: seriesInfo?.description
            this.author = author
        }
    }

    fun image(imageBytes: ByteArray) = transaction {
        val imageBlob = ExposedBlob(imageBytes)

        val img = Image.findOne { TImages.image eq imageBlob }
        img ?: Image.new { image = imageBlob }
    }
}
