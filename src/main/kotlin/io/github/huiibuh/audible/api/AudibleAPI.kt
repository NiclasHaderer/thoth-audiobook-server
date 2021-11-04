package io.github.huiibuh.audible.api

import io.github.huiibuh.audible.models.*
import io.ktor.client.*

class AudibleAPI(
    private val searchHost: String = "audible.de",
    private val authorHost: String = "audible.com",
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
        val handler =
            SearchHandler.fromURL(this.client, this.searchHost, keywords, title, author, narrator, language, pageSize)
        return handler.execute()
    }

    suspend fun getAuthorInfo(authorASIN: String): AudibleAuthor {
        val handler = AuthorHandler.fromURL(this.client, this.authorHost, authorASIN, this.authorImageSize)
        return handler.execute()
    }

    suspend fun getBookInfo(bookASIN: String): AudibleBook {
        val handler = BookHandler.fromUrl(this.client, this.searchHost, bookASIN)
        return handler.execute()
    }

    suspend fun getSeriesInfo(seriesASIN: String): AudibleSeries {
        val handler = SeriesHandler.fromURL(this.client, this.searchHost, seriesASIN)
        return handler.execute()
    }

    fun close() {
        this.client.close()
    }
}
