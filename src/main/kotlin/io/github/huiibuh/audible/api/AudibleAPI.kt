package io.github.huiibuh.audible.api

import io.ktor.client.*


suspend fun main(args: Array<String>) {
    val client = AudibleAPI()
    val response = client.search("eragon")

    println(response.toString())
}


class AudibleAPI(private val host: String = "audible.com") {
    private val client = HttpClient()

    suspend fun search(
        keywords: String? = null,
        title: String? = null,
        author: String? = null,
        narrator: String? = null,
        language: AudibleLanguage? = null,
        pageSize: AudiblePageSize? = null
    ): List<AudibleSearchResult> {
        val handler =
            SearchHandler.fromURL(this.client, this.host, keywords, title, author, narrator, language, pageSize)
        return handler.execute()
    }

    suspend fun getAuthorInfo(
        authorURL: String
    ) {

    }

    suspend fun getBookInfo(
        authorURL: String
    ) {

    }

    fun close() {
        this.client.close()
    }
}
// TODO add id to author, book, series
// TODO write extractors for author, book, series
