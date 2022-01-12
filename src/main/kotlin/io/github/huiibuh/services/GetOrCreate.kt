package io.github.huiibuh.services

import io.github.huiibuh.api.exceptions.ApiException
import io.github.huiibuh.db.tables.Author
import io.github.huiibuh.db.tables.Book
import io.github.huiibuh.db.tables.Image
import io.github.huiibuh.db.tables.ProviderID
import io.github.huiibuh.db.tables.Series
import io.github.huiibuh.db.tables.TAuthors
import io.github.huiibuh.db.tables.TBooks
import io.github.huiibuh.db.tables.TImages
import io.github.huiibuh.db.tables.TSeries
import io.github.huiibuh.extensions.findOne
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.models.ProviderIDModel
import io.github.huiibuh.utils.imageFromString
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

object GetOrCreate : KoinComponent {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val metadataProvider by inject<MetadataProvider>()

    fun author(
        name: String,
        biography: String? = null,
        providerID: ProviderID? = null,
        image: Image? = null,
    ) = transaction {
        var author = Author.findOne { TAuthors.name like name }
        if (author != null) return@transaction author

        val authorInfo = try {
            runBlocking { metadataProvider.getAuthorByName(name) }
        } catch (e: ApiException) {
            log.warn("$e, Author: $name")
            null
        }

        if (authorInfo == null) {
            log.info("Could not find author information for $name")
        }

        author = Author.new {
            this.name = name
            this.biography = biography ?: authorInfo?.biography
            this.providerID = providerID ?: ProviderID.getOrCreate(authorInfo?.id)
            this.image = image ?: if (authorInfo?.image != null) {
                image(runBlocking { imageFromString(authorInfo.image!!) })
            } else null
        }
        return@transaction author
    }

    fun book(
        title: String,
        year: Int?,
        language: String?,
        description: String?,
        providerID: ProviderIDModel?,
        author: String,
        narrator: String?,
        series: String?,
        seriesIndex: Float?,
        cover: ByteArray?,
    ) = transaction {
        val authorItem = author(author)
        val seriesItem = if (series == null) null else series(series, authorItem)
        val book = Book.findOne { TBooks.title like title and (TBooks.author eq authorItem.id.value) }
        book
            ?: Book.new {
                this.title = title
                this.year = year
                this.language = language
                this.description = description
                this.providerID = ProviderID.getOrCreate(providerID)
                this.author = authorItem
                this.narrator = narrator
                this.series = seriesItem
                this.seriesIndex = seriesIndex
                this.cover = if (cover != null) image(cover) else null
            }
    }

    fun series(
        title: String,
        author: Author,
        providerID: ProviderID? = null,
        description: String? = null,
    ) = transaction {
        val series = Series.findOne { TSeries.title like title }
        if (series != null) return@transaction series

        val seriesInfo = try {
            runBlocking { metadataProvider.getSeriesByName(title) }
        } catch (e: ApiException) {
            log.debug("$e, Series: $title")
            null
        }

        if (seriesInfo == null) {
            log.debug("Could not find series information for $title")
        }

        Series.new {
            this.title = title
            this.providerID = providerID ?: if (seriesInfo?.id != null) ProviderID.new {
                this.itemID = seriesInfo.id.itemID
                this.provider = seriesInfo.id.provider
            } else null
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
