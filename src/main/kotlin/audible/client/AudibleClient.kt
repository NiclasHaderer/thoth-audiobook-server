package audible.client

import audible.models.AudibleSearchAmount
import audible.models.AudibleSearchLanguage
import io.github.huiibuh.metadata.AuthorMetadata
import io.github.huiibuh.metadata.BookMetadata
import io.github.huiibuh.metadata.MetadataLanguage
import io.github.huiibuh.metadata.MetadataProvider
import io.github.huiibuh.metadata.MetadataSearchCount
import io.github.huiibuh.metadata.ProviderWithID
import io.github.huiibuh.metadata.SearchResultMetadata
import io.github.huiibuh.metadata.SeriesMetadata
import io.ktor.client.*
import me.xdrop.fuzzywuzzy.FuzzySearch

const val AUDIBLE_PROVIDER_NAME = "audible"

open class AudibleClient(
    private val searchHost: String = "audible.de",
    private val authorHost: String = "audible.de",
    private val authorImageSize: Int = 500,
    private val searchScore: Int = 80,
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
    ): List<SearchResultMetadata> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost,
                                            keywords = keywords,
                                            title = title,
                                            author = author,
                                            narrator = narrator,
                                            language = if (language != null) AudibleSearchLanguage.from(language) else null,
                                            pageSize = if (pageSize != null) AudibleSearchAmount.from(pageSize) else AudibleSearchAmount.Twenty
        )
        return handler.execute()
    }

    override suspend fun getAuthorByID(authorID: ProviderWithID): AuthorMetadata? {
        val handler = AuthorHandler.fromURL(this.client, this.authorHost, authorID.id, this.authorImageSize)
        return handler.execute()
    }

    override suspend fun getAuthorByName(authorName: String): AuthorMetadata? {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, author = authorName)
        val searchResult = handler.execute()
        val authorResult = searchResult.filter { it.author != null && it.author?.id?.id != "search" }
        if (authorResult.isEmpty()) return null

        val author = FuzzySearch.extractOne(authorName, authorResult) { it.author!!.name }
        if (author.score < searchScore) return null

        return getAuthorByID(object : ProviderWithID {
            override val uniqueProviderName = uniqueName
            override val id = author.referent.author!!.id.id
        })
    }

    override suspend fun getBookByName(bookName: String): BookMetadata? {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, title = bookName)
        val searchResult = handler.execute()
        val bookResult = searchResult.filter { it.title != null && it.id.id != "search" }
        if (bookResult.isEmpty()) return null

        val book = FuzzySearch.extractOne(bookName, bookResult) { it.title }
        if (book.score < searchScore) return null

        return getBookByID(object : ProviderWithID {
            override val uniqueProviderName = uniqueName
            override val id = book.referent.id.id
        })
    }

    override suspend fun getBookByID(bookID: ProviderWithID): BookMetadata? {
        val handler = BookHandler.fromUrl(this.client, this.searchHost, bookID.id)
        return handler.execute()
    }

    override suspend fun getSeriesByID(seriesID: ProviderWithID): SeriesMetadata? {
        val handler = SeriesHandler.fromURL(this.client, this.searchHost, seriesID.id)
        return handler.execute()
    }

    override suspend fun getSeriesByName(seriesName: String): SeriesMetadata? {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, keywords = seriesName)
        val searchResult = handler.execute()
        val seriesResult = searchResult.filter { it.series != null && it.series?.id?.id != "search" }
        if (seriesResult.isEmpty()) return null

        val series = FuzzySearch.extractOne(seriesName, seriesResult) { it.series!!.name }
        if (series.score < searchScore) return null

        return getSeriesByID(object : ProviderWithID {
            override val uniqueProviderName = uniqueName
            override val id = series.referent.series!!.id.id
        })
    }

    fun close() {
        this.client.close()
    }
}
