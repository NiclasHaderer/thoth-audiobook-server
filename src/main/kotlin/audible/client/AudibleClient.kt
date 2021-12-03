package audible.client

import audible.models.AudibleAuthor
import audible.models.AudibleBook
import audible.models.AudibleSearchAmount
import audible.models.AudibleSearchLanguage
import audible.models.AudibleSearchResult
import audible.models.AudibleSeries
import io.ktor.client.*

open class AudibleClient(
    private val searchHost: String = "audible.de",
    private val authorHost: String = "audible.de",
    private val authorImageSize: Int = 500,
) {
    private val client = HttpClient()

    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleSearchLanguage? = null,
        pageSize: AudibleSearchAmount? = null,
    ): List<AudibleSearchResult> {
        val handler = SearchHandler.fromURL(this.client, this.searchHost,
                                            keywords = keywords,
                                            title = title,
                                            author = author,
                                            narrator = narrator,
                                            language = language,
                                            pageSize = pageSize)
        return handler.execute()
    }

    suspend fun getAuthorInfo(authorASIN: String): AudibleAuthor {
        val handler = AuthorHandler.fromURL(this.client, this.authorHost, authorASIN, this.authorImageSize)
        return handler.execute()
    }

    suspend fun getAuthorByName(authorName: String): AudibleAuthor? {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, author = authorName)
        val searchResult = handler.execute()
        val authorResult = searchResult.find { it.author != null && it.author?.asin != "search" } ?: return null
        return getAuthorInfo(authorResult.author!!.asin)
    }

    suspend fun getBookInfo(bookASIN: String): AudibleBook {
        val handler = BookHandler.fromUrl(this.client, this.searchHost, bookASIN)
        return handler.execute()
    }

    suspend fun getSeriesInfo(seriesASIN: String): AudibleSeries {
        val handler = SeriesHandler.fromURL(this.client, this.searchHost, seriesASIN)
        return handler.execute()
    }

    suspend fun getSeriesByName(seriesName: String): AudibleSeries? {
        val handler = SearchHandler.fromURL(this.client, this.searchHost, keywords = seriesName)
        val searchResult = handler.execute()
        val seriesResult = searchResult.find { it.series != null && it.series?.asin != "search" } ?: return null
        return getSeriesInfo(seriesResult.series!!.asin)
    }

    fun close() {
        this.client.close()
    }
}
