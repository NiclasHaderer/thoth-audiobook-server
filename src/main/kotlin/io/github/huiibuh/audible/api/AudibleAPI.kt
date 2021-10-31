package io.github.huiibuh.audible.api

import io.ktor.http.*


suspend fun main(args: Array<String>) {
    val client = AudibleAPI()
    val response = client.search("eragon")
    println(response.toString())
}


class AudibleAPI(private val host: String = "audible.com") {

    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleLanguage? = null,
        pageSize: AudiblePageSize? = null
    ): List<AudibleSearchResult> {
        val url = this.buildSearchUrl(keywords, title, author, narrator, language, pageSize)
        return executeSearch(url.toString())
    }

    private fun buildSearchUrl(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleLanguage? = null,
        pageSize: AudiblePageSize? = null
    ): Url {
        val queryParams = ParametersBuilder()
        queryParams.append("ipRedirectOverride", "true")
        if (keywords != null) {
            queryParams.append("keywords", keywords)
        }
        if (title != null) {
            queryParams.append("title", title)
        }
        if (author != null) {
            queryParams.append("author_author", author)
        }
        if (narrator != null) {
            queryParams.append("narrator", narrator)
        }
        if (language != null) {
            queryParams.append("feature_six_browse-bin", language.language.toString())
        }
        if (pageSize != null) {
            queryParams.append("pageSize", pageSize.size.toString())
        }

        val url = URLBuilder(
            protocol = URLProtocol.HTTPS,
            parameters = queryParams,
            host = this.host,
            encodedPath = "/search"
        )

        return url.build()
    }
}
// TODO add id to author, book, series
// TODO write extractors for author, book, series
