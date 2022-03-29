package io.github.huiibuh.metadata.audible.client

import io.github.huiibuh.metadata.MetadataLanguage
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataSearchCount
import io.github.huiibuh.metadata.ProviderWithIDMetadata
import io.ktor.client.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.xdrop.fuzzywuzzy.FuzzySearch

const val AUDIBLE_PROVIDER_NAME = "audible"

open class AudibleClient(
    private val searchHost: String = "audible.de",
    private val authorHost: String = "audible.de",
    private val authorImageSize: Int = 500,
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
    ): List<io.github.huiibuh.metadata.audible.models.AudibleSearchBookImpl> {
        val handler = SearchHandler.fromURL(
            this.client, this.searchHost,
            keywords = keywords,
            title = title,
            author = author,
            narrator = narrator,
            language = if (language != null) io.github.huiibuh.metadata.audible.models.AudibleSearchLanguage.from(
                language
            ) else null,
            pageSize = if (pageSize != null) io.github.huiibuh.metadata.audible.models.AudibleSearchAmount.from(pageSize) else io.github.huiibuh.metadata.audible.models.AudibleSearchAmount.Twenty
        )
        return handler.execute() ?: listOf()
    }

    override suspend fun getAuthorByID(authorID: ProviderWithIDMetadata): io.github.huiibuh.metadata.audible.models.AudibleAuthorImpl? {
        val handler = AuthorHandler.fromURL(this.client, this.authorHost, authorID.itemID, this.authorImageSize)
        return handler.execute()
    }

    override suspend fun getBookByID(bookID: ProviderWithIDMetadata): io.github.huiibuh.metadata.audible.models.AudibleBookImpl? {
        val handler = BookHandler.fromUrl(this.client, this.searchHost, bookID.itemID)
        return handler.execute()
    }

    override suspend fun getSeriesByID(seriesID: ProviderWithIDMetadata): io.github.huiibuh.metadata.audible.models.AudibleSeriesImpl? {
        val handler = SeriesHandler.fromURL(this.client, this.searchHost, seriesID.itemID)
        return handler.execute()
    }


    override suspend fun getAuthorByName(authorName: String): List<io.github.huiibuh.metadata.audible.models.AudibleAuthorImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val authorResult = searchResult.filter { it.author != null && it.author.id.itemID != "search" }

        return coroutineScope {
            FuzzySearch
                .extractSorted(authorName, authorResult) { it.author!!.name }
                .map {
                    async {
                        getAuthorByID(object : ProviderWithIDMetadata {
                            override val provider = uniqueName
                            override val itemID = it.referent.author!!.id.itemID
                        })
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getBookByName(
        bookName: String,
        authorName: String?,
    ): List<io.github.huiibuh.metadata.audible.models.AudibleBookImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, title = bookName, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val bookResult = searchResult.filter { it.title != null && it.id.itemID != "search" }

        return coroutineScope {
            FuzzySearch.extractSorted(bookName, bookResult) { it.title }
                .map {
                    async {
                        getBookByID(object : ProviderWithIDMetadata {
                            override val provider = uniqueName
                            override val itemID = it.referent.id.itemID
                        })
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    override suspend fun getSeriesByName(
        seriesName: String,
        authorName: String?,
    ): List<io.github.huiibuh.metadata.audible.models.AudibleSeriesImpl> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, keywords = seriesName, author = authorName)
        val searchResult = handler.execute() ?: return listOf()
        val seriesResult = searchResult.filter { it.series != null && it.series.id.itemID != "search" }

        return coroutineScope {
            FuzzySearch.extractSorted(seriesName, seriesResult) { it.series!!.name }
                .map {
                    async {
                        getSeriesByID(object : ProviderWithIDMetadata {
                            override val provider = uniqueName
                            override val itemID = it.referent.series!!.id.itemID
                        })
                    }
                }
                .awaitAll()
                .distinctBy { it?.id?.itemID }
                .filterNotNull()
        }
    }

    fun close() {
        this.client.close()
    }
}
