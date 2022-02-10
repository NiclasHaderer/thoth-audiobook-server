package io.github.huiibuh.metadata.audible.client

import io.github.huiibuh.metadata.MetadataLanguage
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataSearchCount
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.github.huiibuh.metadata.audible.models.*
import io.ktor.client.*
import me.xdrop.fuzzywuzzy.FuzzySearch

const val AUDIBLE_PROVIDER_NAME = "metadata"

open class AudibleClient(
    private val searchHost: String = "audible.de",
    private val authorHost: String = "audible.de",
    private val authorImageSize: Int = 500,
    private val searchScore: Int = 80,
    private val byNameSearchAmount: Int = 5
) : MetadataProvider {

    override val uniqueName = AUDIBLE_PROVIDER_NAME
    private val client = HttpClient()

    override suspend fun search(
        keywords: String?,
        title: String?,
        author: String?,
        narrator: String?,
        language: MetadataLanguage?,
        pageSize: MetadataSearchCount?,
    ): List<AudibleSearchBookImpl> {
        val handler = SearchHandler.fromURL(
            this.client, this.searchHost,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = if (language != null) AudibleSearchLanguage.from(language) else null,
            pageSize = if (pageSize != null) AudibleSearchAmount.from(pageSize) else AudibleSearchAmount.Twenty
        )
        return handler.execute() ?: listOf()
    }

    override suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): AudibleAuthorImpl? {
        val handler = AuthorHandler.fromURL(this.client, this.authorHost, authorID.itemID, this.authorImageSize)
        return handler.execute()
    }

    override suspend fun getAuthorByName(authorName: String): List<AudibleAuthorImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val authorResult = searchResult.filter { it.author != null && it.author.id.itemID != "search" }

        return FuzzySearch
            .extractSorted(authorName, authorResult) { it.author!!.name }
            .filter { it.score > searchScore }
            .take(byNameSearchAmount)
            .mapNotNull {
                getAuthorByID(object : ProviderWithIDMetadata {
                    override val provider = uniqueName
                    override val itemID = it.referent.author!!.id.itemID
                })
            }
    }

    override suspend fun getBookByName(bookName: String, authorName: String?): List<AudibleBookImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, title = bookName, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val bookResult = searchResult.filter { it.title != null && it.id.itemID != "search" }

        return FuzzySearch.extractSorted(bookName, bookResult) { it.title }
            .filter { it.score > searchScore }
            .take(byNameSearchAmount)
            .mapNotNull {
                getBookByID(object : ProviderWithIDMetadata {
                    override val provider = uniqueName
                    override val itemID = it.referent.id.itemID
                })
            }
    }

    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): AudibleBookImpl? {
        val handler = BookHandler.fromUrl(this.client, this.searchHost, bookID.itemID)
        return handler.execute()
    }

    override suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): AudibleSeriesImpl? {
        val handler = SeriesHandler.fromURL(this.client, this.searchHost, seriesID.itemID)
        return handler.execute()
    }

    override suspend fun getSeriesByName(seriesName: String, authorName: String?): List<AudibleSeriesImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, keywords = seriesName, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val seriesResult = searchResult.filter { it.series != null && it.series.id.itemID != "search" }

        return FuzzySearch.extractSorted(seriesName, seriesResult) { it.series!!.name }
            .filter { it.score > searchScore }
            .take(byNameSearchAmount)
            .mapNotNull {
                getSeriesByID(object : ProviderWithIDMetadata {
                    override val provider = uniqueName
                    override val itemID = it.referent.series!!.id.itemID
                })
            }
    }

    fun close() {
        this.client.close()
    }
}
